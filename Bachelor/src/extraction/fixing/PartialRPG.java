package extraction.fixing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;

import com.sun.jdi.ObjectReference;

import extraction.ObjectNode;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import util.GraphStructureException;

public class PartialRPG extends SimpleDirectedGraph<ObjectNode, DefaultEdge> {
	private static final long serialVersionUID = 1L;
	public static final int RPG_TYPE_UNBROKEN = 0;
	public static final int RPG_TYPE_MISSING_ROOT = -1;
	public static final int RPG_TYPE_MISSING_SINK = 2;
	public static final int RPG_TYPE_MISSING_LIST_EDGE = 1;
	public static final int RPG_TYPE_MISSING_BACK_EDGE = 3;
	// Start off as an invalid type
	public int type = -20;
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
	
	public RSST getRSST() throws GraphStructureException{
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
			boolean backEdgeMissing = false;
			if (m != 2*n-4){
				throw new GraphStructureException();
			}
			rsst = new RSST();
			ObjectNode v = degree1Nodes.get(0);
			ObjectNode w = degree1Nodes.get(1);
			rsst.sink = degree0Nodes.get(0);
			Iterator<ObjectNode> vBFS = new BreadthFirstIterator<ObjectNode,DefaultEdge>(this,v);
			Set<ObjectNode> reachedNodesV = new HashSet<ObjectNode>();
			ObjectNode currentNode = null;		
			while (vBFS.hasNext()){
				currentNode = vBFS.next();
				reachedNodesV.add(currentNode);
				// If the sink is reachable by any of the 1-outdegree nodes, there can be no missing list edge, so it must be a missing back edge
				if (currentNode == rsst.sink){
					backEdgeMissing = true;
					break;
				}
			}
			if (currentNode == w){
				rsst.root = v;
				rsst.source = w;
			}
			if (backEdgeMissing){
				type = RPG_TYPE_MISSING_BACK_EDGE;
			} else {
//				currentNode = null;
//				Iterator<ObjectNode> wBFS = new BreadthFirstIterator<ObjectNode,DefaultEdge>(this,w);
//				Set<ObjectNode> reachedNodesW = new HashSet<ObjectNode>();
//				while (wBFS.hasNext()){
//					currentNode = wBFS.next();
//				}
//				if (currentNode == v){
//					root = w;
//					source = v;
//				}
//				// If v is not reachable from w or vice versa. Or if both nodes are both not the last one to be reached by the other one. 
//				if ((!reachedNodesV.contains(w)) || (!reachedNodesW.contains(v)) || (root == null)){
//					throw new GraphStructureException();
//				}
//				type = RPG_TYPE_MISSING_LIST_EDGE;
				
				// THIS DOESN'T ACTUALLY WORK YET
				throw new GraphStructureException("Missing list edges other than the first or last one not yet supported.");
			}
		}
		if ((type == RPG_TYPE_MISSING_BACK_EDGE) || (type == RPG_TYPE_MISSING_LIST_EDGE) || (type == RPG_TYPE_MISSING_ROOT)){
			// In all these cases, the sink has been found
			DirectedGraph<ObjectNode,DefaultEdge> transposedGraph = new EdgeReversedGraph<ObjectNode,DefaultEdge>(this);
			Iterator<ObjectNode> sinkIter = new DepthFirstIterator<ObjectNode,DefaultEdge>(transposedGraph,rsst.sink);
			ObjectNode lastNode = rsst.sink;
			ObjectNode currentNode = null;
			while (sinkIter.hasNext()){
				currentNode = sinkIter.next();
				if (lastNode == currentNode){
					continue;
				}
				// There should only ever be one choice to go, leading straight to the wanted node.
				if (!transposedGraph.containsEdge(lastNode, currentNode)){
					throw new GraphStructureException();
				}
				lastNode = currentNode;
			}
			// and the last node found is the one we want
			
			switch(type) {
			case RPG_TYPE_MISSING_BACK_EDGE : rsst.root = currentNode;
			     break;
			default : rsst.target = currentNode;
			}
		}		
		return rsst;
	}
	
	// Doesn't actually do anything yet.
	public List<ObjectNode> reconstructHamiltonPath() throws Exception{
		RSST rsst = getRSST();
		return null;
	}
	
	/**
	 * type must already be set
	 * @param rsst
	 * @return
	 */
	public boolean checkIfRPGUnbroken(RSST rsst){
		// This is probably nonsense, but necessary for now until I implement the fixing of edges
		switch(type) {
		case RPG_TYPE_UNBROKEN : 
			return checkIfRPGUnbrokenOrMissingBackEdge(rsst);
		case RPG_TYPE_MISSING_BACK_EDGE : 
			return checkIfRPGUnbrokenOrMissingBackEdge(rsst);
		case RPG_TYPE_MISSING_ROOT : 
			return checkIfRPGMissingRoot(rsst);
		case RPG_TYPE_MISSING_SINK : 
			return checkIfRPGMissingSink(rsst);
		case RPG_TYPE_MISSING_LIST_EDGE : 
			return checkIfRPGMissingListEdge(rsst);
		default : return false;
		}
	}
	
	private boolean checkIfRPGMissingRoot(RSST rsst){
		return false;
	}
	
	private boolean checkIfRPGMissingSink(RSST rsst){
		return false;
	}
	
	private boolean checkIfRPGMissingListEdge(RSST rsst){
		return false;
	}
	
	// Let's just get this out of the way to at least get it working again.
	private boolean checkIfRPGUnbrokenOrMissingBackEdge(RSST rsst){
		ObjectNode root = rsst.root;
		HashMap<ObjectReference,ObjectNode> refToNode = new HashMap<ObjectReference,ObjectNode>();
		Set<ObjectReference> previousObjects = new HashSet<ObjectReference>();
		for (ObjectNode node : vertexSet()){
			refToNode.put(node.or, node);
		}
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
			previousObjects.add(lastNode.or);
			int nodeType = currentNode.isValidBodyNode(previousObjects);
			if ((nodeType == ObjectNode.BODY_NODE_UNBROKEN) || (nodeType == ObjectNode.BODY_NODE_MISSING_BACK_EDGE)){
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
		if ((currentNode.isValidFootNode()) && (size >= 5) && (size % 2 == 1)){
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
		return null;
	}
	/**
	 * If this graph is presumably missing the sink, this method gets the root and sink of this graph as well as the source of the missing edge. It also creates a dummy node
	 * with outdegree 0, sets it as target and adds the edge between source and target.
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
			// Again, the root has indegree >= 2, but the second to  last node does not.
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
		result.target = ObjectNode.createDummyNode();
		// ... and connect the second to last one to our dummy node. This graph is now fixed.
		addEdge(result.source, result.target);
		return result;
	}
}
