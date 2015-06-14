package embedding;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class TreeNeighborMap extends HashMap<Integer, Integer> {
	DirectedGraph<Integer,DefaultEdge> _graph;
	List<Integer> _nodes;
	public TreeNeighborMap(List<Integer> nodes, DirectedGraph<Integer,DefaultEdge> graph){
		super();
		this._graph = graph;
		this._nodes = nodes;
		outNeighbors();
	}
	
	private void outNeighbors(){
		Set<DefaultEdge> outEdges;
		Integer outNeighbor = null;
		for (Integer vertex : _nodes){
			outEdges = _graph.outgoingEdgesOf(vertex);
			for (DefaultEdge edge : outEdges){
				// each vertex has only two outNeighbors; the larger one is correct 
				outNeighbor = _graph.getEdgeTarget(edge);
				if (outNeighbor > vertex){
					break;
				}
			}
			this.put(vertex, outNeighbor);
		}
	}
}
