package extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.sun.jdi.ObjectReference;

/**
 * This class is for extracting RPG graphs embedded in a program with the method of Chroni and Nikolopoulos.
 * @author Kaspar
 *
 */
public class Extractor {
	
	private final String _classPath;
	private final String[] _args;
	private final String _className;
	private ExtractVMLauncher launcher;
	
	/**
	 * Constructs a new Extractor.
	 * @param classPath The classpath by which the class may be executed.
	 * @param className the fully qualified name of the main class.
	 * @param args The arguments to the main class' main method.
	 */
	
	public Extractor(String classPath, String className, String[] args) {
		this._className = className;
		this._classPath = classPath;
		this._args = args;
	}
	
	/**
	 * Executes the given main class, extracting all embedded RPGs.
	 * @return A list of all RPGS that are embedded in the program.
	 */
	public List<DirectedGraph<Integer,DefaultEdge>> run(){
		List<ObjectReference> constructedObjects = extractConstructedObjects();
		List<ObjectNode> possibleRoots = getPossibleRoots(constructedObjects);
		List<DirectedGraph<Integer,DefaultEdge>> rpgList = getPossibleGraphs(possibleRoots);
		return rpgList;
	}
	
	/**
	 * Given a list of possible root nodes, this method goes through a depth-first search on the embedded graph, starting with
	 * the provided root nodes, adding, for each root, nodes to a DirectedGraph as it encounters new ObjectReferences. It also tests
	 * if the found structure can be interpreted as an embedded RPG.
	 * @param possibleRoots The possible root nodes from where the search starts.
	 * @return A list of possibly embedded graphs.
	 */
	private List<DirectedGraph<Integer,DefaultEdge>> getPossibleGraphs(List<ObjectNode> possibleRoots){
		List<DirectedGraph<Integer,DefaultEdge>> graphList = new ArrayList<DirectedGraph<Integer,DefaultEdge>>();
		for (ObjectNode root : possibleRoots){
			// This is for checking if a newly found ObjectReference is actually already in the vertex set without
			// having to construct an ObjectNode first
			Map<ObjectReference,Integer> objectToIndex = new HashMap<ObjectReference,Integer>();
			// A running index for the vertices; this has nothing to do with the RPG numbering
			Integer index = 0;
			DirectedGraph<Integer,DefaultEdge> result = new SimpleDirectedGraph<Integer,DefaultEdge>(DefaultEdge.class);
			
			objectToIndex.put(root.or, index);
			result.addVertex(index);
			
			boolean success = false;
			
			// root nodes only have one child at index 0
			ObjectReference nextObject = root.getChildren().get(0);
			ObjectNode lastNode = root;
			ObjectNode currentNode = ObjectNode.checkIfValidRPGNode(nextObject);
			while (currentNode != null){
				index++;
				ObjectReference currentObject = currentNode.or;
				objectToIndex.put(currentObject, index);
				result.addVertex(index);
				
				// add edges from lastNode; this is possible because all outneighbors of lastNode should
				// now have corresponding indices
				Integer lastNodeIndex = objectToIndex.get(lastNode.or);
				Integer currentNodeIndex = new Integer(index);
				// add list edge
				result.addEdge(lastNodeIndex, currentNodeIndex);
				if (lastNode.isValidBodyNode()){
					// add back/tree edge
					Integer backNeighborIndex = objectToIndex.get(lastNode.getChildren().get(1));
					result.addEdge(lastNodeIndex, backNeighborIndex);
				}
				if (!currentNode.isValidBodyNode()){
					if (currentNode.isValidFootNode()){
						success = true;
					} else {
						success = false;
					}
					break;
				}
				
				// The outneighbors of currentNode
				// Since nextNode is a valid body node, these two are both not null 
				ObjectReference link1 = currentNode.getChildren().get(0);
				ObjectReference link2 = currentNode.getChildren().get(1);
				
				boolean link1Null = (objectToIndex.get(link1) == null);
				boolean link2Null = (objectToIndex.get(link2) == null);
				// If link1 is via back (e2) node, but link2 isn't
				if ((!link1Null) && (link2Null)){
					lastNode = currentNode;
					currentNode = ObjectNode.checkIfValidRPGNode(link2);
				} else if ((!link2Null) && (link1Null)){
					lastNode = currentNode;
					currentNode = ObjectNode.checkIfValidRPGNode(link1);
				} else {
					// If both are null, there is no tree edge
					// If both are not-null, there is no list edge
					success = false;
					break;
				}
			}
			int size = result.vertexSet().size();
			if ((success) && (size >= 5) && (size % 2 == 1)){
				graphList.add(result);
			}
		}
		return graphList;
	}
	
	/**
	 * Executes the main class and gives back the last 1000 constructed objects.
	 * @return A list of the last 1000 newly constructed ObjectReferences in the target VM.
	 */
	private List<ObjectReference> extractConstructedObjects(){
		launcher = new ExtractVMLauncher(_classPath,_className,_args);
		ObjectConstructionThread oct = launcher.getObjectConstructionThread();
		
		while (oct.isAlive()){}
		RingBuffer<ObjectReference> constructedObjects = null;
		try {
			constructedObjects = oct.getObjects();
		} catch (Exception e) {
			// can't happen because we specifically wait for oct to die
		}
		return new ArrayList<ObjectReference>(constructedObjects);
	}
	
	/**
	 * Goes through a list of ObjectReferences, trying to find ones that could serve as root nodes to an RPG.
	 * @param constructedObjects A list of ObjectReferences where possible roots are hopefully stored in.
	 * @return A list of possible root nodes.
	 */
	private List<ObjectNode> getPossibleRoots(List<ObjectReference> constructedObjects){
		List<ObjectNode> possibleRoots = new ArrayList<ObjectNode>();
		for (ObjectReference or : constructedObjects){
			ObjectNode node = ObjectNode.checkIfValidRPGNode(or);
			if ((node != null) && (node.isValidRootNode())){
				possibleRoots.add(node);
			}
		}
		return possibleRoots;
	}
	
	/**
	 * Finally disposes of the VM, letting it run its course. This <strong>must</strong> be called after the graphs have been extracted, 
	 * otherwise this program will not terminate.
	 */
	public void quitVM(){
		// Let the VM run its course, finally finishing its cruelly prolonged death
		launcher.getVM().dispose();
	}

	public static void main(String[] args){
		Extractor ext = new Extractor(".", "example.Example", new String[] {"test"});
		List<DirectedGraph<Integer,DefaultEdge>> rpgList = ext.run();
		System.out.println(rpgList);
		ext.quitVM();
	}
}
