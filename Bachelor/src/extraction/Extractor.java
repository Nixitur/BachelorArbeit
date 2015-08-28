package extraction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import extraction.fixing.HeapGraph;
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
	public Set<Set<Integer>> run(){
		List<ObjectNode> constructedNodes = extractConstructedNodes(RingBuffer.UNLIMITED_SIZE);
		Set<ObjectNode> nodeSet = new HashSet<ObjectNode>(constructedNodes);
		Set<Set<Integer>> rootChildrenSets = new HashSet<Set<Integer>>();
		try {
			HeapGraph heapGraph = new HeapGraph(nodeSet);
			Set<PartialRPG> subgraphs = heapGraph.getConnectedComponents();
			for (PartialRPG rpg : subgraphs){
				Set<Integer> rootChildren = rpg.checkForIntegrityAndGetRootChildren();
				if (rootChildren != null){
					rootChildrenSets.add(rootChildren);
				} else {
					System.out.println("Not an RPG");
				}
			}
			return rootChildrenSets;
		} catch (Exception e) {
			// This REALLY shouldn't happen.
			e.printStackTrace();
			return null;
		}
	}

	
	/**
	 * Executes the main class and gives back the last <tt>size</tt> constructed objects as nodes.
	 * @param size The size of the ring buffer that is used for storing nodes.
	 * @return A list of the last newly constructed ObjectReferences, converted to ObjectNodes.
	 */
	private List<ObjectNode> extractConstructedNodes(int size){
		launcher = new ExtractVMLauncher(_classPath,_className,_args, size);
		ObjectConstructionThread oct = launcher.getObjectConstructionThread();
		
		while (oct.isAlive()){}
		try {
			return oct.getNodes();
		} catch (Exception e) {			
			// can't happen because we specifically wait for oct to die
			return null;
		}
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
		Set<Set<Integer>> rootChildrenSets = ext.run();
		System.out.println(rootChildrenSets);
		ext.quitVM();
	}
}
