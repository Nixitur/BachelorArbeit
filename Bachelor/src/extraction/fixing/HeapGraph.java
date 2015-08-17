package extraction.fixing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import util.GraphStructureException;

import com.sun.jdi.ObjectReference;

import extraction.ObjectNode;

public class HeapGraph extends
		SimpleDirectedGraph<ObjectNode, DefaultEdge> {
	private static final long serialVersionUID = -8646807887457172709L;
	
	public HeapGraph(Set<ObjectNode> constructedNodes) throws Exception {
		super(DefaultEdge.class);
		HashMap<ObjectReference,ObjectNode> referenceToNode = new HashMap<ObjectReference,ObjectNode>();
		for (ObjectNode node : constructedNodes){
			referenceToNode.put(node.or, node);
		}
		
		for (ObjectNode thisNode : constructedNodes){
			this.addVertex(thisNode);
			for (ObjectReference childRef : thisNode.getChildren()){
				// childNode needs to actually be in constructedNodes or not all nodes have been provided
				ObjectNode childNode = referenceToNode.get(childRef);
				if (childNode != null){
					// Graphs can never contain duplicate vertices, so adding this is harmless, but necessary to ensure successful edge addition
					this.addVertex(childNode);
					this.addEdge(thisNode, childNode);
				} else {
					throw new Exception("A node has been found with a child node not in the argument set. constructedNodes needs to contain "+
											"all nodes of the graph.");
				}
			}
		}
	}
	
	public Set<PartialRPG> getConnectedComponents(){
		Set<PartialRPG> result = new HashSet<PartialRPG>();
		// We need to look at the undirected graph because we want to find the weakly connected components
		AsUndirectedGraph<ObjectNode, DefaultEdge> undirectedGraph = new AsUndirectedGraph<ObjectNode, DefaultEdge>(this);
		Set<ObjectNode> foundNodes = new HashSet<ObjectNode>();
		for (ObjectNode vertex : this.vertexSet()){
			if (!(foundNodes.contains(vertex))){
				// vertex is now the start node for the DFS, iterating over vertices in the undirected graph 
				Iterator<ObjectNode> iter = new DepthFirstIterator<ObjectNode,DefaultEdge>(undirectedGraph,vertex);
				Set<ObjectNode> subgraphVertices = new HashSet<ObjectNode>();
				while (iter.hasNext()){
					subgraphVertices.add(iter.next());
				}
				foundNodes.addAll(subgraphVertices);
				
				DirectedSubgraph<ObjectNode, DefaultEdge> inducedSubgraph = new DirectedSubgraph<ObjectNode, DefaultEdge>(
						this,subgraphVertices,null);
				try {
					PartialRPG partialRPG = new PartialRPG(inducedSubgraph);
					result.add(partialRPG);
				} catch (GraphStructureException e) {
					// If what we provide is not even a broken RPG, just ignore it. This includes graphs with just one node which MIGHT be a part of an RPG
					// but just aren't too helpful.
				}
			}
		}
		return result;
	}

}
