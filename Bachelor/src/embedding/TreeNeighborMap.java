package embedding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Instances of this class take a reducible permutation graph and map each node in a given <code>List</code> of nodes
 * to a <code>List</code> of in-neighbors of that node via tree edges. 
 * @author Kaspar
 *
 */
class TreeNeighborMap extends HashMap<Integer, List<Integer>> {
	private static final long serialVersionUID = 1L;
	DirectedGraph<Integer,DefaultEdge> _graph;
	List<Integer> _nodes;
	
	/**
	 * Creates a new instance which maps the nodes in <code>nodes</code> to their in neighbors via tree edges in RPG <code>graph</code>.
	 * @param nodes the nodes to be mapped, each to a <code>List</code> of its in-neighbors via tree edges
	 * @param graph the reducible permutation graph
	 */
	TreeNeighborMap(List<Integer> nodes, DirectedGraph<Integer,DefaultEdge> graph){
		super();
		this._graph = graph;
		this._nodes = nodes;
		inNeighbors();
	}
	
	/**
	 * Calculates the in-neighbors of the nodes in <code>_nodes</code> via tree edges.
	 */
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
	
//	/**
//	 * Returns 
//	 * @return
//	 */
//	Set<Integer> getAllInNeighbors(){
//		Set<Integer> result = new HashSet<Integer>();
//		for (List<Integer> inNeighbors : this.values()){
//			result.addAll(inNeighbors);
//		}
//		return result;
//	}
}
