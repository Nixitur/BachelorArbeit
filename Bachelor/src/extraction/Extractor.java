package extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.sun.jdi.ObjectReference;

import extraction.fixing.PartialRPG;

/**
 * This class is for extracting RPG graphs embedded in a program with the method of Chroni and Nikolopoulos.
 * @author Kaspar
 *
 */
public class Extractor {
	//TODO: Basically, allow "root" to also refer to the SECOND node, a body node, instead of the root node, to deal with cases where the first edge is missing.
	
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
	public List<PartialRPG> run(){
		List<ObjectNode> constructedNodes = extractConstructedNodes();
		List<ObjectNode> possibleRoots = getPossibleRoots(constructedNodes);
		List<PartialRPG> rpgList = getPossibleGraphs(possibleRoots);
		return rpgList;
	}
	
	/**
	 * Given a list of possible root nodes, this method goes through a depth-first search on the embedded graph, starting with
	 * the provided root nodes, adding, for each root, nodes to a DirectedGraph as it encounters new ObjectReferences. It also tests
	 * if the found structure can be interpreted as an embedded RPG.
	 * @param possibleRoots The possible root nodes from where the search starts.
	 * @return A list of possibly embedded graphs.
	 */
	private List<PartialRPG> getPossibleGraphs(List<ObjectNode> possibleRoots){
		List<PartialRPG> graphList = new ArrayList<PartialRPG>();
		for (ObjectNode root : possibleRoots){
			// This is for checking if a newly found ObjectReference is actually already in the vertex set without
			// having to construct an ObjectNode first
			Map<ObjectReference,Integer> objectToIndex = new HashMap<ObjectReference,Integer>();
			// A running index for the vertices; this has nothing to do with the RPG numbering
			Integer index = 0;
			PartialRPG result = new PartialRPG();
			
			objectToIndex.put(root.or, index);
			result.addVertex(index);
			
			boolean success = false;
			
			// root nodes only have one child at index 0
			ObjectReference nextObject = root.getChildren().get(0);
			ObjectNode lastNode = root;
			ObjectNode currentNode = ObjectNode.checkIfValidRPGNode(nextObject);
			while (currentNode != null){				
				index++;
				// The index for the currentNode
				result.addVertex(index);
				
				// add edges from lastNode; this is possible because all outneighbors of lastNode should
				// now have corresponding indices
				Integer lastNodeIndex = objectToIndex.get(lastNode.or);
				Integer currentNodeIndex = new Integer(index);
				// add list edge
				result.addEdge(lastNodeIndex, currentNodeIndex);
				// objectToIndex currently contains all objects up to lastNode; to check if it's unbroken, we need to exclude lastNode
				objectToIndex.remove(lastNode.or);
				int bodyNodeType = lastNode.isValidBodyNode(objectToIndex.keySet());
				if (bodyNodeType == ObjectNode.BODY_NODE_UNBROKEN){
					// add back/tree edge
					Integer backNeighborIndex = objectToIndex.get(lastNode.getChildren().get(1));
					result.addEdge(lastNodeIndex, backNeighborIndex);
				}
				// and add it back in
				objectToIndex.put(lastNode.or, lastNodeIndex);
				bodyNodeType = currentNode.isValidBodyNode(objectToIndex.keySet());

				ObjectReference currentObject = currentNode.or;
				objectToIndex.put(currentObject, index);
				
				if (bodyNodeType != ObjectNode.BODY_NODE_UNBROKEN){
					// If the current node is a foot node, just stop right there as we've found it all
					if (currentNode.isValidFootNode()){
						success = true;
						break;
					}
					// If this body node can not be fixed or is just not a body node, stop
					if (bodyNodeType == ObjectNode.BODY_NODE_WRONG){
						success = false;
						break;
					}
					// If this partial RPG is already missing an edge and this body node is also missing an edge, we can't fix it
					if (result.type != PartialRPG.RPG_TYPE_UNBROKEN){
						success = false;
						break;
					}
					// From here on out, the RPG is, as of now, unbroken and currentNode is a body node with a missing edge
					
					// If this body node is missing a back edge, we can still continue and find the rest of the nodes 
					if (bodyNodeType == ObjectNode.BODY_NODE_MISSING_BACK_EDGE){
						result.type = PartialRPG.RPG_TYPE_MISSING_BACK_EDGE;
					// If this body node is missing a list edge, we can NOT continue, so we break here
					} else if (bodyNodeType == ObjectNode.BODY_NODE_MISSING_LIST_EDGE){
						result.type = PartialRPG.RPG_TYPE_MISSING_LIST_EDGE;
						success = true;
						break;
					}
				}
				// At this point, currentNode is either unbroken or missing a back edge
				ObjectReference listNeighbor = currentNode.getListNeighbor(objectToIndex.keySet());
				ObjectReference backNeighbor = currentNode.getBackNeighbor(objectToIndex.keySet());
				
				if (listNeighbor != null){
					lastNode = currentNode;
					currentNode = ObjectNode.checkIfValidRPGNode(listNeighbor);
				} else {
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
	 * Executes the main class and gives back the last 1000 constructed objects as nodes.
	 * @return A list of the last 1000 newly constructed ObjectReferences, converted to ObjectNodes.
	 */
	private List<ObjectNode> extractConstructedNodes(){
		launcher = new ExtractVMLauncher(_classPath,_className,_args);
		ObjectConstructionThread oct = launcher.getObjectConstructionThread();
		
		while (oct.isAlive()){}
		RingBuffer<ObjectReference> constructedObjects = null;
		List<ObjectNode> constructedNodes = new ArrayList<ObjectNode>();
		try {
			constructedObjects = oct.getObjects();
			ObjectNode node = null;
			for (ObjectReference or : constructedObjects){
				node = ObjectNode.checkIfValidRPGNode(or);
				if (node != null){
					constructedNodes.add(node);
				}
			}
		} catch (Exception e) {			
			// can't happen because we specifically wait for oct to die
			return null;
		}
		return constructedNodes;
	}
	
	/**
	 * Goes through a list of ObjectReferences, trying to find ones that could serve as root nodes to an RPG.
	 * @param constructedObjects A list of ObjectReferences where possible roots are hopefully stored in.
	 * @return A list of possible root nodes.
	 */
	private List<ObjectNode> getPossibleRoots(List<ObjectNode> constructedNodes){
		List<ObjectNode> possibleRoots = new ArrayList<ObjectNode>();
		for (ObjectNode node : constructedNodes){
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
		List<PartialRPG> rpgList = ext.run();
		System.out.println(rpgList);
		ext.quitVM();
	}
}
