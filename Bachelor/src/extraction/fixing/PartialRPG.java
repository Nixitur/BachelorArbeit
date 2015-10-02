package extraction.fixing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import util.GraphStructureException;
import util.TimeKeeper;

public class PartialRPG extends SimpleDirectedGraph<Integer, DefaultEdge> {
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
	
	private List<Integer> hamiltonPath = null;
	
	public PartialRPG() {
		super(DefaultEdge.class);
	}
	
	/**
	 * Creates a new PartialRPG from an existing DirectedGraph, containing all its vertices and edges. This graph is not backed by
	 * the underlying graph or the other way around, so changes to either will not reflect on the other.
	 * @param <V>
	 * @param graph The existing graph.
	 * @throws GraphStructureException If the provided graph is not an RPG.
	 */
	public <V> PartialRPG(DirectedGraph<V,DefaultEdge> graph) throws GraphStructureException{		
		super(DefaultEdge.class);
		HashMap<V,Integer> nodeToInt = new HashMap<V,Integer>();
		// the vertex names don't much matter in this graph, so just assign them randomly
		Integer i = 0;
		for (V node : graph.vertexSet()){
			nodeToInt.put(node, i);
			addVertex(i);
			i++;
		}
		for (DefaultEdge e : graph.edgeSet()){
			V source = graph.getEdgeSource(e);
			V target = graph.getEdgeTarget(e);
			addEdge(nodeToInt.get(source),nodeToInt.get(target));
		}
		int n = vertexSet().size();
		int m = edgeSet().size();
		// m = 2n-3 if unbroken, 2n-4 if any one edge but the one to the sink is missing and 2n-2 if the sink is missing
		// Also, n must be at least 4 in which case w = 1 and the sink is missing
		if ((m < 2*n-4) || (m > 2*n-2) || (n < 4)){
			throw new GraphStructureException();
		}
	}
	
	/**
	 * Checks whether this graph is an RPG and returns the children of the root node.
	 * @return The children of the root node if this is a fixable RPG, <tt>null</tt> otherwise.
	 */
	public Set<Integer> checkForIntegrityAndGetRootChildren(){		
		try {
			TimeKeeper time = new TimeKeeper("Hamilton path");
			// Sets the type field correctly and finds root and sink of the graph as well as source and target of missing list edge
			RSST rsst = getRSST();
			// Fix missing list edge if possible and store Hamilton Path
			this.hamiltonPath = reconstructHamiltonPath(rsst);
			time.stop();
			// With the list edges fixed, the final and most precise RPG-check can commence
			if (!checkIfRPG(rsst.root)){
				return null;
			}
			time = new TimeKeeper("rename and tree edges");
			RepresentativeForest forest = new RepresentativeForest(this,hamiltonPath);
			Set<Integer> rootChildren = forest.getRootChildren();
			time.stop();
			return rootChildren;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Gets the root and sink of this RPG as well as the source and target of a missing edge, if possible. 
	 * @return An RSST object holding root, sink, source and target, if it's possible to extract those. 
	 * @throws GraphStructureException If any of those four vertices can not be extracted, either due to this graph not being an RPG or just being too broken.
	 */
	private RSST getRSST() throws GraphStructureException{
		int n = vertexSet().size();
		int m = edgeSet().size();
		
		List<Integer> degree0Nodes = new ArrayList<Integer>();
		List<Integer> degree1Nodes = new ArrayList<Integer>();
		for (Integer node : vertexSet()){
			if (outDegreeOf(node) == 0){
				degree0Nodes.add(node);
			} else if (outDegreeOf(node) == 1){
				degree1Nodes.add(node);
			}
		}
		RSST rsst = null;
		
		if ((degree0Nodes.size() == 1) && (degree1Nodes.size() == 1)){
			// unbroken case
			if ((m != 2*n-3) || (n % 2 == 0)){
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
			// any other list edge or any back edge missing; also n = 2*nBento+3, so it must be odd
			if ((m != 2*n-4) || (n % 2 == 0)){
				throw new GraphStructureException();
			}
			rsst = new RSST();
			Integer v = degree1Nodes.get(0);
			rsst.sink = degree0Nodes.get(0);
			if (isBackEdgeMissing(v,rsst.sink)){
				type = RPG_TYPE_MISSING_BACK_EDGE;
			} else {
				type = RPG_TYPE_MISSING_LIST_EDGE;
			}
		} else {
			throw new GraphStructureException();
		}
		if ((type == RPG_TYPE_MISSING_BACK_EDGE) || (type == RPG_TYPE_MISSING_LIST_EDGE) || (type == RPG_TYPE_MISSING_ROOT)){
			// In all these cases, the sink has been found			
			List<Integer> nodesFromSink = getDFSOnReversedGraph(rsst.sink);
			int sizeOfDFS = nodesFromSink.size();
			// The last node found, starting from sink
			Integer currentNode = nodesFromSink.get(sizeOfDFS - 1);
			
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
					Integer nodeNPlus1 = nodesFromSink.get(nBento+1);
					Integer nodeN = nodesFromSink.get(nBento);
					rsst.root = findRootMissingListEdge(nodeNPlus1, nodeN);
					// degree1Nodes is exactly the root and the source
					degree1Nodes.remove(rsst.root);
					rsst.source = degree1Nodes.get(0);
				} else {
					Set<Integer> firstHalfNodes = new HashSet<Integer>(vertexSet());
					firstHalfNodes.removeAll(nodesFromSink);
					DirectedGraph<Integer,DefaultEdge> firstHalf = new DirectedSubgraph<Integer,DefaultEdge>(this,firstHalfNodes,null);
					int node0degree = firstHalf.inDegreeOf(degree1Nodes.get(0));
					int node1degree = firstHalf.inDegreeOf(degree1Nodes.get(1));
					// If sizeOfDFS is too small, then n+1 MUST be in the first half
					// n+1 is guaranteed to have an edge to the root, so the root's indegree in this subgraph is > 1.
					// However, the other node can only have indegree 1, from exactly the node that comes before it in the HP
					if((node0degree == 1) && (node1degree > 1)){
						rsst.root = degree1Nodes.get(1);
						rsst.source = degree1Nodes.get(0);
					} else if ((node1degree == 1) && (node0degree > 1)){
						rsst.root = degree1Nodes.get(0);
						rsst.source = degree1Nodes.get(1);
					} else {
						throw new GraphStructureException();
					}
				}
			}
		}
		return rsst;
	}

	/**
	 * Finds the root node if this graph is missing a list edge.
	 * @param nodeNPlus1 The node n+1, using the labels of Bento et al.
	 * @param nodeN THe node n, using the labels of Bento et al.
	 * @return The root node of this graph.
	 * @throws GraphStructureException If no root can be found, either due to this graph just being too broken or just not being an RPG.
	 */
	private Integer findRootMissingListEdge(Integer nodeNPlus1, Integer nodeN) throws GraphStructureException{
		// there must be an edge connecting n+1 and n
		DefaultEdge listEdge = getEdge(nodeNPlus1, nodeN);
		if (listEdge == null){
			throw new GraphStructureException();
		}
		Integer result = null;
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
	
	/**
	 * If this graph is either missing a list edge (other than the first or last) or a back edge, this method finds out whether it is missing a back edge.
	 * @param outDegree1Node One of the two nodes with outdegree 1.
	 * @param sink The sink of this RPG.
	 * @return <tt>true</tt> if this is missing a back edge, <tt>false</tt> otherwise.
	 * @throws GraphStructureException If outDegree1Node does not have outdegree 1 or if sink does not have outdegree 0.
	 */
	private boolean isBackEdgeMissing(Integer outDegree1Node, Integer sink) throws GraphStructureException{
		if ((outDegreeOf(outDegree1Node) != 1) || (outDegreeOf(sink) != 0)){
			throw new GraphStructureException();
		}
		Iterator<Integer> iter = new DepthFirstIterator<Integer,DefaultEdge>(this,outDegree1Node);
		Integer currentNode = null;		
		while (iter.hasNext()){
			currentNode = iter.next();
			// If the sink is reachable by any of the 1-outdegree nodes, there can be no missing list edge, so it must be a missing back edge
			if (currentNode == sink){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Performs Depth First Search on the transposed graph of this graph, starting from the sink.
	 * @param sink This graph's sink.
	 * @return The nodes in the order in which they have been encountered in the DFS.
	 * @throws GraphStructureException If sink does not have outdegree 0, i.e. is not actually the sink.
	 */
	private List<Integer> getDFSOnReversedGraph(Integer sink) throws GraphStructureException{
		if (outDegreeOf(sink) != 0){
			throw new GraphStructureException();
		}
		List<Integer> reverseNodeList = new ArrayList<Integer>();
		DirectedGraph<Integer,DefaultEdge> transposedGraph = new EdgeReversedGraph<Integer,DefaultEdge>(this);
		Iterator<Integer> sinkIter = new DepthFirstIterator<Integer,DefaultEdge>(transposedGraph,sink);
		Integer lastNode = sink;
		Integer currentNode = null;
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
	private List<Integer> reconstructHamiltonPath(RSST rsst) throws GraphStructureException{
		// In both of these cases, source and target are not set
		// In the former because it's not necessary, in the latter because it's complicated
		if ((type != RPG_TYPE_UNBROKEN) && (type != RPG_TYPE_MISSING_BACK_EDGE)){
			this.addEdge(rsst.source, rsst.target);
		}
		// In any case, all list edges should now be fixed
		List<Integer> result = new ArrayList<Integer>();
		Iterator<Integer> rootIter = new DepthFirstIterator<Integer,DefaultEdge>(this,rsst.root);
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
	private boolean checkIfRPG(Integer root) throws Exception{
		if ((root == null) || (!isValidRootNode(root))){
			throw new Exception("Provided node is not a valid root node.");
		}
		Set<Integer> previousObjects = new HashSet<Integer>();
		Iterator<Integer> rootIter = new DepthFirstIterator<Integer,DefaultEdge>(this,root);
		Integer lastNode = root;
		Integer currentNode = root;
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
	private RSST getRSSTMissingRoot(List<Integer> degree0Nodes, List<Integer> degree1Nodes) throws GraphStructureException{
		int n = vertexSet().size();
		int m = edgeSet().size();
		RSST result = new RSST();
		// the number of vertices must be odd in this case
		if ((m != 2*n-4) || (n % 2 == 0)){
			throw new GraphStructureException();
		}
		int index = -1;
		for (int i = 0; i < 2; i++){
			Integer node = degree0Nodes.get(i);
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
	private RSST getRSSTMissingSink(List<Integer> degree0Nodes, List<Integer> degree1Nodes) throws GraphStructureException{
		int n = vertexSet().size();
		int m = edgeSet().size();
		RSST result = new RSST();
		// the number of vertices must be even in this case
		if ((m != 2*n-2) || (n % 2 == 1)){
			throw new GraphStructureException();
		}
		int index = -1;
		for (int i = 0; i < 2; i++){
			Integer node = degree1Nodes.get(i);
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
		Integer dummyNode = Integer.MIN_VALUE;
		this.addVertex(dummyNode);
		result.target = dummyNode;
		result.sink = dummyNode;
		return result;
	}
	
	/**
	 * Checks if the provided node is a valid or potentially broken (by removal of one edge) body node.
	 * @param node The node to be checked.
	 * @param previous The nodes that have already been found and are thus valid targets for a back edge in an RPG.
	 * @return BODY_NODE_WRONG if this node is not a valid body node and can not be fixed to be one.<br>
	 * 		   BODY_NODE_UNBROKEN if this node is a valid body node that has not been tampered with, i.e. one edge points
	 * 		   back, one points forward.<br>
	 * 		   BODY_NODE_NO_LIST_POINTER if this node could potentially be a body node that had its list edge removed.<br>
	 * 		   BODY_NODE_NO_BACK_POINTER if this node could potentially be a body node that had its back edge removed.
	 */
	private int isValidBodyNode(Integer node, Set<Integer> previous){
		Set<DefaultEdge> edges = outgoingEdgesOf(node);
		List<Integer> children = new ArrayList<Integer>();
		for (DefaultEdge edge : edges){
			children.add(getEdgeTarget(edge));
		}
		// A body node needs at least one pointer to be fixable
		if ((children.size() == 0) || (children.size() > 2)){
			return BODY_NODE_WRONG;
		}
		Integer child0 = children.get(0);
		if (children.size() == 2){
			Integer child1 = children.get(1);
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
	 * Checks if the node is a valid root node, i.e. has exactly one outneighbor.
	 * @param node The node to be checked.
	 * @return <tt>true</tt> if it's a valid root node, false otherwise.
	 */
	private boolean isValidRootNode(Integer node){
		Set<DefaultEdge> edges = outgoingEdgesOf(node);
		return (edges.size() == 1);
	}
	
	/**
	 * Checks if the provided node is a valid foot node, i.e. has exactly zero outneighbors.
	 * @param node The node to be checked.
	 * @return <tt>true</tt> if it's a valid foot node, false otherwise.
	 */
	private boolean isValidFootNode(Integer node){
		Set<DefaultEdge> edges = outgoingEdgesOf(node);
		return (edges.size() == 0);
	}
}
