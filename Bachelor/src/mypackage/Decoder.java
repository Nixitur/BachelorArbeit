package mypackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public class Decoder{
	private DirectedGraph<Integer,DefaultEdge> rpg;
	private List<Integer> hamiltonPath;
	private DirectedGraph<Integer,DefaultEdge> tree;
	private Integer startNode;
	
	/**
	 * Instances of this class are decoders for a given reducible permutation graph.
	 * @param rpg The RPG to be decoded.
	 * @throws GraphStructureException If <code>rpg</code> is not an RPG encoded with the algorithm of Chroni and Nikolopoulos.
	 */
	public Decoder(DirectedGraph<Integer,DefaultEdge> rpg) throws GraphStructureException{
		this.rpg = rpg;
		this.hamiltonPath = decodeRPGToHamiltonPath();
		this.rpg = renameVertices();
		this.startNode = hamiltonPath.get(0);
		this.tree = decodeRPGToTree();
	}
	
	/**
	 * Renames the vertices of <code>rpg</code> in accordance with the unique Hamilton path
	 *   <code>hamiltonPath</code>. For <code>n</code> vertices, the vertex labels go from
	 *   <code>n-2</code> to <code>-1</code>. This makes it robust against relabeling.
	 */
	private DirectedGraph<Integer,DefaultEdge> renameVertices(){
		Map<Integer,Integer> vertexNames = new HashMap<Integer,Integer>();
		int size = hamiltonPath.size();
		Integer key;
		// vertexNames maps the original vertex labels to the intended ones
		for (int i = 0; i < size; i++){
			key = hamiltonPath.get(i);
			vertexNames.put(key, size-i-2);
		}
		DirectedGraph<Integer,DefaultEdge> newGraph = new SimpleDirectedGraph<Integer,DefaultEdge>(DefaultEdge.class);
		// Fill newGraph with correctly labeled vertices 
		for (Integer vertex : rpg.vertexSet()){
			newGraph.addVertex(vertexNames.get(vertex));
		}
		Integer source;
		Integer target;
		// Fill newGraph with correctly labeled edges
		for (DefaultEdge edge : rpg.edgeSet()){
			source = rpg.getEdgeSource(edge);
			target = rpg.getEdgeTarget(edge);
			newGraph.addEdge(vertexNames.get(source), vertexNames.get(target));
		}
		return newGraph;
	}
	
	/**
	 * Decodes a reducible permutation graph to the self-inverting permutation it encodes.
	 * @return The SIP that is encoded in the given RPG. It is 0-indexed instead of 1-indexed.
	 * @throws GraphStructureException If the graph is not a proper RPG.
	 */
	public List<Integer> decodeRPGToSIP() throws GraphStructureException{		
		List<Integer> preord = preorderTraversal(startNode,null);
		preord.remove(0);
		return preord;
	}
	
	/**
	 * Decodes the RPG with the algorithm of Bento et al.
	 * @return The number that is encoded in the given RPG.
	 * @throws GraphStructureException
	 */
	public int decodeRPGBento(){
		// The rpg has exactly 2n+3 nodes, from 2n+1 to -1
		int n = (hamiltonPath.size() - 3) / 2;
		List<Integer> children = new ArrayList<Integer>();
		for (DefaultEdge edge : tree.outgoingEdgesOf(startNode)){
			children.add(tree.getEdgeTarget(edge));
		}
		int sum = 0;
		// Since Bento is 1- instead of 0-indexed, we have to subtract 1
		for (Integer child : children){
			sum = (int) (sum + (Math.pow(2, 2*n - child - 1)));
		}
		return sum;
	}
	
	/**
	 * Does a preorder traversal on a tree as defined by Bento et al.
	 * @param startNode The root of the current subtree.
	 * @param discovered A list of all vertices that have been discovered so far. <code>null</code> if no
	 *   vertices have as yet been discovered.
	 * @return <code>startNode</code> if <code>tree</code> consists of one vertex<br>
	 *   <code>startNode</code> followed by the preorder traversals of the rooted trees with the children
	 *   of <code>startNode</code> as roots  
	 * @throws GraphStructureException If <code>tree</code> contains loops.
	 */
	private List<Integer> preorderTraversal(Integer startNode, List<Integer> discovered)
			throws GraphStructureException{
				
		if (discovered == null){
			discovered = new ArrayList<Integer>();
		} else if (discovered.contains(startNode)){
			throw new GraphStructureException();
		}
		List<Integer> result = new ArrayList<Integer>();
		discovered.add(startNode);
		result.add(startNode);
		List<DefaultEdge> edgeList = new ArrayList<DefaultEdge>(tree.outgoingEdgesOf(startNode));
		List<Integer> children = new ArrayList<Integer>();
		for (DefaultEdge edge : edgeList){
			children.add(tree.getEdgeTarget(edge));
		}
		Collections.sort(children);
		Integer child;
		for (int i = 0; i < children.size(); i++){
			child = children.get(i);
			result.addAll(preorderTraversal(child,discovered));			
		}
		return result;
	}
	
	/**
	 * Returns the depth-first traversal of an RPG starting at the header node. This also happens to be
	 * exactly the Hamilton path of the graph.
	 * @return A list of all the nodes in <code>g</code>, starting at the header and ending at the footer. 
	 * @throws GraphStructureException Thrown if <code>g</code> is not a proper RPG as encoded by the
	 *      algorithm of Chroni and Nikolopoulos.
	 */
	private List<Integer> decodeRPGToHamiltonPath() throws GraphStructureException {
		Integer header = null;
		Integer footer = null;
		
		List<Integer> vertices = new ArrayList<Integer>(rpg.vertexSet());
		int outDegree;
		for (Integer vertex : vertices){
			outDegree = rpg.outDegreeOf(vertex);
			if ((outDegree == 1) && (header == null)){
				header = vertex;
			} else if ((outDegree == 0) && (footer == null)){
				footer = vertex;
			} else if (outDegree != 2){
				// There can be only one (header or footer) and only out-degrees up to 2 are allowed.
				throw new GraphStructureException("Argument is not a proper RPG.");
			}
		}
		
		// Header and footer MUST exist. The smallest size of the RPG is 5 and it's always odd. 
		if ((header == null) || (footer == null)
				|| (vertices.size() < 5) || (vertices.size() % 2 == 0)){
			throw new GraphStructureException("Argument is not a proper RPG.");
		}
		vertices.clear();
		Iterator<Integer> iter = new DepthFirstIterator<Integer,DefaultEdge>(rpg,header);
		Integer vertex;
		while (iter.hasNext()){
			vertex = iter.next();
			vertices.add(vertex);
		}
		return vertices;
	}
	
	/**
	 * Turns an RPG into its representative tree as per the paper by Bento et al.
	 * @return The representative tree of <code>g</code>.
	 */
	private DirectedGraph<Integer,DefaultEdge> decodeRPGToTree() {
		// Don't want to overwrite hamiltonPath
		List<Integer> hPath = new ArrayList<Integer>(hamiltonPath);
		// Remove the last node
		Integer footer = hPath.remove(hPath.size()-1);
		rpg.removeVertex(footer);
		// Remove all list edges
		for (int i = 1; i < hPath.size(); i++){
			rpg.removeEdge(hPath.get(i-1), hPath.get(i));
		}
		return new EdgeReversedGraph<Integer,DefaultEdge>(rpg);		
	}
}