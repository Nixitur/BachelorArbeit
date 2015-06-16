package encoding;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.*;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.*;

/**
 * Displays a graph in JGraph
 * @author Kaspar
 *
 * @param <V> Type of the vertex in the graph. Must be comparable to position correctly.
 */
public class GraphVisualizer<V extends Comparable<? super V>> extends JFrame{
	private static final long serialVersionUID = 1L;
	private JGraph jgraph;
	private DirectedGraph<V,DefaultEdge> g;
	private JGraphModelAdapter<V, DefaultEdge> jgAdapter;
	
	/**
	 * Constructs a window containing a JGraph for a given graph.
	 * @param g The <code>DirectedGraph</code> to display
	 */
	public GraphVisualizer(DirectedGraph<V,DefaultEdge> g){
		super("Graph Visualizer");
		this.g = g;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
		ListenableGraph<V,DefaultEdge> graph = new ListenableDirectedGraph<V,DefaultEdge>(g);
		jgAdapter = new JGraphModelAdapter<V,DefaultEdge>(graph);
		this.jgraph = new JGraph(jgAdapter);
		getContentPane().add(jgraph);
		positionVertices();
		pack();
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Positions the vertices of the graph in a linear fashion
	 */
	private void positionVertices(){
		List<V> vertexList = new ArrayList<V>(g.vertexSet());
		Collections.sort(vertexList);
		int n = vertexList.size();
		V vertex = vertexList.get(0);
		DefaultGraphCell cell = jgAdapter.getVertexCell(vertex);
		AttributeMap attr = cell.getAttributes();
		Rectangle2D bounds = GraphConstants.getBounds(attr);
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		for (int i = 0; i < n; i++){
			vertex = vertexList.get(n-i-1);
			cell = jgAdapter.getVertexCell(vertex);
			attr = cell.getAttributes();
			Rectangle2D newBounds = new Rectangle2D.Double(
					i * 2 * width,
					0,
					width,
					height);
			GraphConstants.setBounds(attr, newBounds);
			AttributeMap cellAttr = new AttributeMap();
			cellAttr.put(cell, attr);
			jgAdapter.edit(cellAttr, null, null, null);
		}
	}
}
