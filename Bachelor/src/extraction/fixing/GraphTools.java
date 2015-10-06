package extraction.fixing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.traverse.DepthFirstIterator;

public class GraphTools {

	private GraphTools() {}
	
	public static <V> Set<DirectedGraph<V,DefaultEdge>> getConnectedComponents(DirectedGraph<V,DefaultEdge> graph){
		Set<DirectedGraph<V,DefaultEdge>> result = new HashSet<DirectedGraph<V,DefaultEdge>>();
		// We need to look at the undirected graph because we want to find the weakly connected components
		AsUndirectedGraph<V, DefaultEdge> undirectedGraph = new AsUndirectedGraph<V, DefaultEdge>(graph);
		Set<V> foundNodes = new HashSet<V>();
		for (V vertex : graph.vertexSet()){
			if (!(foundNodes.contains(vertex))){
				// vertex is now the start node for the DFS, iterating over vertices in the undirected graph 
				Iterator<V> iter = new DepthFirstIterator<V,DefaultEdge>(undirectedGraph,vertex);
				Set<V> subgraphVertices = new HashSet<V>();
				while (iter.hasNext()){
					subgraphVertices.add(iter.next());
				}
				foundNodes.addAll(subgraphVertices);
				// The following is an induced subgraph over the vertices subgraphVertices of the graph graph, NOT the undirectedGraph
				DirectedSubgraph<V, DefaultEdge> inducedSubgraph = new DirectedSubgraph<V, DefaultEdge>(
						graph,subgraphVertices,null);
				result.add(inducedSubgraph);
			}
		}
		return result;
	}
	
	public static <V> HashMap<V,Integer> renameNodes(List<V> hamiltonPath){
		HashMap<V,Integer> result = new HashMap<V,Integer>();
		// 2n+3 nodes, the highest one is 2n+2
		int index = hamiltonPath.size() - 1;
		for (V node : hamiltonPath){
			result.put(node, index);
			index--;
		}
		return result;
	}
}
