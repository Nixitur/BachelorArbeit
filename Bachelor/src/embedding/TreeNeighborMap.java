package embedding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class TreeNeighborMap extends HashMap<Integer, List<Integer>> {
	private static final long serialVersionUID = 1L;
	DirectedGraph<Integer,DefaultEdge> _graph;
	List<Integer> _nodes;
	
	public TreeNeighborMap(List<Integer> nodes, DirectedGraph<Integer,DefaultEdge> graph){
		super();
		this._graph = graph;
		this._nodes = nodes;
		inNeighbors();
	}
	
	private void inNeighbors(){
		Set<DefaultEdge> inEdges;
		List<Integer> inNeighbors;
		Integer inNeighbor;
		for (Integer vertex : _nodes){
			inNeighbors = new ArrayList<Integer>();
			inEdges = _graph.incomingEdgesOf(vertex);
			for (DefaultEdge edge : inEdges){
				// each vertex can have many inNeighbors; only the smaller ones are correct as the larger is a list edge
				inNeighbor = _graph.getEdgeSource(edge);
				if (inNeighbor < vertex){
					inNeighbors.add(inNeighbor);
				}
			}
			this.put(vertex, inNeighbors);
		}
	}
	
	public Set<Integer> getAllInNeighbors(){
		Set<Integer> result = new HashSet<Integer>();
		for (List<Integer> inNeighbors : this.values()){
			result.addAll(inNeighbors);
		}
		return result;
	}
}
