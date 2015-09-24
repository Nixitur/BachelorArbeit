package extraction.fixing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
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
			addVertex(thisNode);
			for (ObjectReference childRef : thisNode.getChildren()){
				// childNode needs to actually be in constructedNodes or not all nodes have been provided
				ObjectNode childNode = referenceToNode.get(childRef);
				if (childNode != null){
					// Graphs can never contain duplicate vertices, so adding this is harmless, but necessary to ensure successful edge addition
					addVertex(childNode);
					// loops and multiple vertices are not allowed
					if ((containsEdge(thisNode,childNode)) || (thisNode == childNode)){
						removeVertex(childNode);
					} else {
						addEdge(thisNode, childNode);
					}
				} else {
					throw new Exception("A node has been found with a child node not in the argument set. constructedNodes needs to contain "+
											"all nodes of the graph.");
				}
			}
		}
	}
	
	public Set<PartialRPG> getConnectedComponents(){
		Set<PartialRPG> result = new HashSet<PartialRPG>();
		Set<DirectedGraph<ObjectNode,DefaultEdge>> subgraphs = GraphTools.getConnectedComponents(this);
		for (DirectedGraph<ObjectNode,DefaultEdge> subgraph : subgraphs){
			try {
				PartialRPG partialRPG = new PartialRPG(subgraph);
				result.add(partialRPG);
			} catch (GraphStructureException e) {
				// If what we provide is not even a broken RPG, just ignore it.
			}
		}
		return result;
	}

}
