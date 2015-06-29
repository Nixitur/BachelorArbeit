package example;

import java.io.IOException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class Main {

	public static void main(String[] args) throws IOException{
		int w = 28;
		String packageName = "example";
		int[] sip = encoding.Encode.encodeWToSIP(w);
		DirectedGraph<Integer, DefaultEdge> graph = encoding.Encode.encodeSIPtoRPG(sip);
		
		embedding.Embedder embedder = new embedding.Embedder(".", "example.Example", new String[] {"test"}, "example.Marker.mark");
		int noOfTracePoints = embedder.run();		
		
		embedding.WatermarkCreator wmark = new embedding.WatermarkCreator(packageName, graph, noOfTracePoints);
		int noOfBuildMethods = wmark.create();
		String wmarkClassName = wmark.getClassName();		
		
		embedder.dump(wmarkClassName, noOfBuildMethods);
	}
}
