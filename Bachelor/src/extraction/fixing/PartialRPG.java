package extraction.fixing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;

import extraction.ObjectNode;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import util.GraphStructureException;

public class PartialRPG extends SimpleDirectedGraph<ObjectNode, DefaultEdge> {
	private static final long serialVersionUID = 230563163974817736L;
	
	public static final int RPG_TYPE_UNBROKEN = 0;
	public static final int RPG_TYPE_MISSING_ROOT = -1;
	public static final int RPG_TYPE_MISSING_SINK = 2;
	public static final int RPG_TYPE_MISSING_LIST_EDGE = 1;
	public static final int RPG_TYPE_MISSING_BACK_EDGE = 3;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's a body node that has not been tampered with.
	 */
	private static final int BODY_NODE_UNBROKEN = 1;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's a body node that had its list edge removed.
	 */
	private static final int BODY_NODE_MISSING_LIST_EDGE = 2;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's a body node that had its back edge removed.
	 */
	private static final int BODY_NODE_MISSING_BACK_EDGE = 3;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's not a valid body node.
	 */
	private static final int BODY_NODE_WRONG = 4;
	// Start off as an invalid type
	public int type = -20;
	
	private List<ObjectNode> hamiltonPath = null;
	
	public PartialRPG() {
		super(DefaultEdge.class);
	}
	
	/**
	 * Creates a new PartialRPG from an existing DirectedGraph, containing all its vertices and edges. This graph is not backed by
	 * the underlying graph or the other way around, so changes to either will not reflect on the other.
	 * @param graph The existing graph.
	 * @throws GraphStructureException If the provided graph is not an RPG.
	 */
	public PartialRPG(DirectedGraph<ObjectNode,DefaultEdge> graph) throws GraphStructureException{		
		super(DefaultEdge.class);
		int n = graph.vertexSet().size();
		int m = graph.edgeSet().size();
		// m = 2n-3 if unbroken, 2n-4 if any one edge but the one to the sink is missing and 2n-2 if the sink is missing
		if ((m < 2*n-4) || (m > 2*n-2)){
			throw new GraphStructureException();
		}
		for (ObjectNode node : graph.vertexSet()){
			this.addVertex(node);
		}
		for (DefaultEdge edge : graph.edgeSet()){
			this.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
		}
	}
	
	/**
	 * Reconstructs a missing list edge if it's the only missing edge and checks whether this graph is an RPG.
	 * @return true if this is an RPG, false otherwise
	 */
	public boolean checkForIntegrityAndFix(){		
		try {
			// Sets the type field correctly and finds root and sink of the graph as well as source and target of missing list edge
			RSST rsst = getRSST();
			// Fix missing list edge if possible and store Hamilton Path
			this.hamiltonPath = reconstructHamiltonPath(rsst);
			// With the list edges fixed, the final and most precise RPG-check can commence
			return checkIfRPG(rsst.root);
		} catch (Exception e) {
			return false;
		}
		// TODO: Add fixing of tree edge which is complicated and requires all the Bento stuff
	}
	
	private RSST getRSST() throws GraphStructureException{
		int n = vertexSet().size();
		int m = edgeSet().size();
		
		List<ObjectNode> degree0Nodes = new ArrayList<ObjectNode>();
		List<ObjectNode> degree1Nodes = new ArrayList<ObjectNode>();
		for (ObjectNode node : vertexSet()){
			if (outDegreeOf(node) == 0){
				degree0Nodes.add(node);
			} else if (outDegreeOf(node) == 1){
				degree1Nodes.add(node);
			}
		}
		RSST rsst = null;
		
		if ((degree0Nodes.size() == 1) && (degree1Nodes.size() == 1)){
			// unbroken case
			if (m != 2*n-3){
				throw new GraphStructureException();
			}
			rsst = new RSST(degree1Nodes.get(0), degree0Nodes.get(0), null, null);
			type = RPG_TYPE_UNBROKEN;
		} else if ((degree0Nodes.size() == 2) && (degree1Nodes.size() == 0)){
			// first list edge missing
			rsst = getRSSTMissingRoot(degree0Nodes, degree1Nodes);
			type = RPG_TYPE_MISSING_ROOT;
		} else if ((degree0Nodes.size() == 0) && (degree1Nodes.size() == 2)){
			// last list edge missing
			rsst = getRSSTMissingSink(degree0Nodes, degree1Nodes);
			type = RPG_TYPE_MISSING_SINK;
		} else if ((degree0Nodes.size() == 1) && (degree1Nodes.size() == 2)){
			// any other list edge or any back edge missing
			if (m != 2*n-4){
				throw new GraphStructureException();
			}
			rsst = new RSST();
			ObjectNode v = degree1Nodes.get(0);
			rsst.sink = degree0Nodes.get(0);
			if (isBackEdgeMissing(v,rsst.sink)){
				type = RPG_TYPE_MISSING_BACK_EDGE;
			} else {
				type = RPG_TYPE_MISSING_LIST_EDGE;
			}
		}
		if ((type == RPG_TYPE_MISSING_BACK_EDGE) || (type == RPG_TYPE_MISSING_LIST_EDGE) || (type == RPG_TYPE_MISSING_ROOT)){
			// In all these cases, the sink has been found			
			List<ObjectNode> nodesFromSink = getDFSOnReversedGraph(rsst.sink);
			int sizeOfDFS = nodesFromSink.size();
			// The last node found, starting from sink
			ObjectNode currentNode = nodesFromSink.get(sizeOfDFS - 1);
			
			switch(type) {
			// If we're missing a back edge, going backwards from the sink gets us to the root
			case RPG_TYPE_MISSING_BACK_EDGE : rsst.root = currentNode;
			     break;
			// Otherwise, we get to the target of a missing list edge
			default : rsst.target = currentNode;
			}
			
			// If the "right half" of the RPG is large enough, the node n+1 can be found which has an edge to root
			if ((type == RPG_TYPE_MISSING_LIST_EDGE)){
				// This is the "n" as it's used in the paper of Bento et al., i. e., the vertices go from 0 to 2n+2.
				int nBento = (n-3)/2;
				if (sizeOfDFS > nBento+1){
					ObjectNode nodeNPlus1 = nodesFromSink.get(nBento+1);
					ObjectNode nodeN = nodesFromSink.get(nBento);
					rsst.root = findRootMissingListEdge(nodeNPlus1, nodeN);
					// degree1Nodes is exactly the root and the source
					degree1Nodes.remove(rsst.root);
					rsst.source = degree1Nodes.get(0);
				} else {
					throw new GraphStructureException("This graph can not be fixed at the moment as it's missing a list edge to close to the sink.");
				}
			}
		}
		return rsst;
	}
	
	private ObjectNode findRootMissingListEdge(ObjectNode nodeNPlus1, ObjectNode nodeN) throws GraphStructureException{
		// there must be an edge connecting n+1 and n
		DefaultEdge listEdge = getEdge(nodeNPlus1, nodeN);
		if (listEdge == null){
			throw new GraphStructureException();
		}
		ObjectNode result = null;
		Set<DefaultEdge> edgesFromNPlus1 = outgoingEdgesOf(nodeNPlus1);
		boolean rootSet = false;
		for (DefaultEdge e : edgesFromNPlus1){
			if (!e.equals(listEdge)){
				if (rootSet){
					throw new GraphStructureException();
				}
				rootSet = true;
				// n+1 is guaranteed to have an edge to the root. It has one other edge, the one to n.
				result =  this.getEdgeTarget(e);
			}
		}
		if (!rootSet){
			throw new GraphStructureException();
		}
		return result;
	}
	
	private boolean isBackEdgeMissing(ObjectNode outDegree1Node, ObjectNode sink) throws GraphStructureException{
		if ((outDegreeOf(outDegree1Node) != 1) || (outDegreeOf(sink) != 0)){
			throw new GraphStructureException();
		}
		Iterator<ObjectNode> iter = new DepthFirstIterator<ObjectNode,DefaultEdge>(this,outDegree1Node);
		ObjectNode currentNode = null;		
		while (iter.hasNext()){
			currentNode = iter.next();
			// If the sink is reachable by any of the 1-outdegree nodes, there can be no missing list edge, so it must be a missing back edge
			if (currentNode == sink){
				return true;
			}
		}
		return false;
	}
	
	private List<ObjectNode> getDFSOnReversedGraph(ObjectNode sink) throws GraphStructureException{
		if (outDegreeOf(sink) != 0){
			throw new GraphStructureException();
		}
		List<ObjectNode> reverseNodeList = new ArrayList<ObjectNode>();
		DirectedGraph<ObjectNode,DefaultEdge> transposedGraph = new EdgeReversedGraph<ObjectNode,DefaultEdge>(this);
		Iterator<ObjectNode> sinkIter = new DepthFirstIterator<ObjectNode,DefaultEdge>(transposedGraph,sink);
		ObjectNode lastNode = sink;
		ObjectNode currentNode = null;
		reverseNodeList.add(sink);
		while (sinkIter.hasNext()){
			currentNode = sinkIter.next();
			if (lastNode == currentNode){
				continue;
			}
			reverseNodeList.add(currentNode);
			// There should only ever be one choice to go, leading straight to the wanted node.
			if (!transposedGraph.containsEdge(lastNode, currentNode)){
				throw new GraphStructureException();
			}
			lastNode = currentNode;
		}
		return reverseNodeList;
	}
	
	/**
	 * Fixes a missing list edge, if possible, and returns the Hamilton path.
	 * @return The Hamilton path.
	 * @throws GraphStructureException If the fixing of the graph isn't possible, either due to it not being an RPG or one that is too complicated to fix.
	 */
	private List<ObjectNode> reconstructHamiltonPath(RSST rsst) throws GraphStructureException{
		// In both of these cases, source and target are not set
		// In the former because it's not necessary, in the latter because it's complicated
		if ((type != RPG_TYPE_UNBROKEN) && (type != RPG_TYPE_MISSING_BACK_EDGE)){
			this.addEdge(rsst.source, rsst.target);
		}
		// In any case, all list edges should now be fixed
		List<ObjectNode> result = new ArrayList<ObjectNode>();
		Iterator<ObjectNode> rootIter = new DepthFirstIterator<ObjectNode,DefaultEdge>(this,rsst.root);
		while (rootIter.hasNext()){
			result.add(rootIter.next());
		}
		return result;
	}
	
	/**
	 * Checks if this graph is an RPG with up to one back edge missing.
	 * @param root The root node of this graph.
	 * @return <tt>true</tt> if this graph is an RPG with up to one back edge missing, <tt>false</tt> otherwise.
	 * @throws Exception If the provided node is not a valid root node of this graph.
	 */
	private boolean checkIfRPG(ObjectNode root) throws Exception{
		if ((root == null) || (!isValidRootNode(root))){
			throw new Exception("Provided node is not a valid root node.");
		}
		Set<ObjectNode> previousObjects = new HashSet<ObjectNode>();
		Iterator<ObjectNode> rootIter = new DepthFirstIterator<ObjectNode,DefaultEdge>(this,root);
		ObjectNode lastNode = root;
		ObjectNode currentNode = root;
		while (rootIter.hasNext()){
			// currentNode starts off as rootNode itself
			currentNode  = rootIter.next();
			if (lastNode == currentNode){
				continue;
			}
			if (!containsEdge(lastNode,currentNode)){
				return false;
			}
			previousObjects.add(lastNode);
			int nodeType = isValidBodyNode(currentNode,previousObjects);
			if ((nodeType == BODY_NODE_UNBROKEN) || (nodeType == BODY_NODE_MISSING_BACK_EDGE)){
				lastNode = currentNode;
			} else {
				// If this non-body node is not the last node
				if (rootIter.hasNext()){
					return false;
				}
			}
		}		
		int size = this.vertexSet().size();
		// We've gotten to the end, so the current node must be a foot node
		if ((isValidFootNode(currentNode)) && (size >= 5) && (size % 2 == 1)){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * If this graph is presumably missing the first list edge, this method gets the root and sink of this graph as well as the source of the missing edge (the root itself). 
	 * @param degree0Nodes list of nodes with outdegree 0.
	 * @param degree1Nodes list of nodes with outdegree 1.
	 * @return A RSST 4-tupel with root, sink and source set and target set to null.
	 * @throws GraphStructureException In some cases where the provided graph is not an RPG. This method doesn't catch all cases, only some of the most obvious ones.
	 */
	private RSST getRSSTMissingRoot(List<ObjectNode> degree0Nodes, List<ObjectNode> degree1Nodes) throws GraphStructureException{
		int n = vertexSet().size();
		int m = edgeSet().size();
		RSST result = new RSST();
		if (m != 2*n-4){
			throw new GraphStructureException();
		}
		int index = -1;
		for (int i = 0; i < 2; i++){
			ObjectNode node = degree0Nodes.get(i);
			// As per Bento et al, the root node has indegree >= 2. The sink obviously does not.
			if (inDegreeOf(node) >= 2){
				if (result.root == null){
					result.root = node;
					result.source = node;
					index = i;
				} else {
					// If the other one also has such a large indegree
					throw new GraphStructureException();
				}
			}
		}
		// If no such node has been found
		if (result.root == null){
			throw new GraphStructureException();
		}
		index = (index+1) % 2;
		// The other one is the sink.
		result.sink = degree0Nodes.get(index);
		return result;
	}
	/**
	 * If this graph is presumably missing the sink, this method gets the root and sink of this graph as well as the source of the missing edge. It also creates a dummy node
	 * with outdegree 0 and sets it as target and root.
	 * @param degree0Nodes list of nodes with outdegree 0.
	 * @param degree1Nodes list of nodes with outdegree 1.
	 * @return A RSST 4-tupel with root, sink, source and target set.
	 * @throws GraphStructureException In some cases where the provided graph is not an RPG. This method doesn't catch all cases, only some of the most obvious ones.
	 */
	private RSST getRSSTMissingSink(List<ObjectNode> degree0Nodes, List<ObjectNode> degree1Nodes) throws GraphStructureException{
		int n = vertexSet().size();
		int m = edgeSet().size();
		RSST result = new RSST();
		if (m != 2*n-2){
			throw new GraphStructureException();
		}
		int index = -1;
		for (int i = 0; i < 2; i++){
			ObjectNode node = degree1Nodes.get(i);
			// The root has indegree >= 2, but the second to last node does not.
			if (inDegreeOf(node) >= 2){
				if (result.root == null){
					result.root = node;
					index = i;
				} else {
					throw new GraphStructureException();
				}
			}
		}
		if (result.root == null){
			throw new GraphStructureException();
		}
		index = (index+1) % 2;
		// The other node is the second to last one in the RPG.
		result.source = degree1Nodes.get(index);
		// The last one exists somewhere, but is impossible to find, so let's just make a dummy node...
		ObjectNode dummyNode = ObjectNode.createDummyNode();
		this.addVertex(dummyNode);
		result.target = dummyNode;
		result.sink = dummyNode;
		return result;
	}
	
	/**
	 * Checks if the provided ObjectNode is a valid or potentially broken (by removal of one edge) body node.
	 * @param node The node to be checked.
	 * @param previous The nodes that have already been found and are thus valid targets for a back edge in an RPG.
	 * @return BODY_NODE_WRONG if this ObjectNode is not a valid body node and can not be fixed to be one.<br>
	 * 		   BODY_NODE_UNBROKEN if this ObjectNode is a valid body node that has not been tampered with, i.e. one edge points
	 * 		   back, one points forward.<br>
	 * 		   BODY_NODE_NO_LIST_POINTER if this ObjectNode could potentially be a body node that had its list edge removed.<br>
	 * 		   BODY_NODE_NO_BACK_POINTER if this ObjectNode could potentially be a body node that had its back edge removed.
	 */
	private int isValidBodyNode(ObjectNode node, Set<ObjectNode> previous){
		Set<DefaultEdge> edges = outgoingEdgesOf(node);
		List<ObjectNode> children = new ArrayList<ObjectNode>();
		for (DefaultEdge edge : edges){
			children.add(getEdgeTarget(edge));
		}
		// A body node needs at least one pointer to be fixable
		if ((children.size() == 0) || (children.size() > 2)){
			return BODY_NODE_WRONG;
		}
		ObjectNode child0 = children.get(0);
		if (children.size() == 2){
			ObjectNode child1 = children.get(1);
			// One pointer needs to point back in previous, the other one needs to point forward to the unknown
			if (((previous.contains(child0)) && (!previous.contains(child1))) ||
				((previous.contains(child1)) && (!previous.contains(child0)))) {
				return BODY_NODE_UNBROKEN;
			}
			// If there's two children, but none or both of them point backwards, it is not a body node.
			return BODY_NODE_WRONG;
		}
		// We know there's only one child.
		if (previous.contains(child0)){
			// If the only child has already been found, there is no list edge
			return BODY_NODE_MISSING_LIST_EDGE;
		} else {
			// If the only child has not been found, there is no back edge
			return BODY_NODE_MISSING_BACK_EDGE;
		}
	}
	
	/**
	 * Checks if the ObjectNode is a valid root node, i.e. has exactly one outneighbor.
	 * @param node The node to be checked.
	 * @return <tt>true</tt> if it's a valid root node, false otherwise.
	 */
	private boolean isValidRootNode(ObjectNode node){
		Set<DefaultEdge> edges = outgoingEdgesOf(node);
		return (edges.size() == 1);
	}
	
	/**
	 * Checks if the provided ObjectNode is a valid foot node, i.e. has exactly zero outneighbors.
	 * @param node The node to be checked.
	 * @return <tt>true</tt> if it's a valid foot node, false otherwise.
	 */
	private boolean isValidFootNode(ObjectNode node){
		Set<DefaultEdge> edges = outgoingEdgesOf(node);
		return (edges.size() == 0);
	}
	
	public List<ObjectNode> getHamiltonPath(){
		return hamiltonPath;
	}
}
