package example;

import java.io.IOException;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import util.GraphStructureException;
import encoding.Decoder;
import extraction.Extractor;
import extraction.ObjectNode;
import extraction.fixing.PartialRPG;

public class Main {

	public static void main(String[] args) throws IOException{
		if ((args.length == 0) || (!(args[0].equals("encode")) && !(args[0].equals("decode"))) ){
			System.out.println("Please enter either \"encode\" to encode and embed the watermark or \"decode\" to extract and decode "
					+ "the watermark.");
			return;
		}
		if (args[0].equals("encode")){
			int w = 5;
			String packageName = "example";
			int[] sip = encoding.Encode.encodeWToSIP(w);
			DirectedGraph<Integer, DefaultEdge> graph = encoding.Encode.encodeSIPtoRPG(sip);

			embedding.Embedder embedder = new embedding.Embedder(".", "example.Example", new String[] {"test"}, "example.Marker.mark");
			int noOfTracePoints = embedder.run();		

			// Specifically only make three build-methods to make it easier to check if it's working
			// TODO: This is only for testing purposes.
			embedding.WatermarkCreator wmark = new embedding.WatermarkCreator(packageName, graph, 3);
			int noOfBuildMethods = wmark.create();
			// Delete the first list edge, thus making it miss the root
			wmark.deleteListEdge(graph.vertexSet().size() - 2);
			try {
				wmark.dump();
			} catch (IllegalStateException e) {
				// Can never happen because it creates first.
				e.printStackTrace();
			}
			String wmarkClassName = wmark.getClassName();		

			embedder.dump(wmarkClassName, noOfBuildMethods);
		}
		if (args[0].equals("decode")){
			Extractor ext = new Extractor(".", "example.Example", new String[] {"test"});
			Set<PartialRPG> rpgList = ext.run();
			ext.quitVM();

			for (PartialRPG rpg : rpgList){
				try {
					Decoder<ObjectNode> decoder = new Decoder<ObjectNode>(rpg);
					int wNew = decoder.decodeRPGBento();
					System.out.println("The embedded watermark is possibly "+wNew);
				} catch (GraphStructureException e) {
					// ignore and go on to the next one
				}
			}
		}
	}
}
