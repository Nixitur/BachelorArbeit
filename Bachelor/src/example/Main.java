package example;

import java.io.IOException;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import encoding.Decoder;
import encoding.GraphStructureException;
import extraction.Extractor;

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

			embedding.WatermarkCreator wmark = new embedding.WatermarkCreator(packageName, graph, noOfTracePoints);
			int noOfBuildMethods = wmark.create();
			String wmarkClassName = wmark.getClassName();		

			embedder.dump(wmarkClassName, noOfBuildMethods);
		}
		if (args[0].equals("decode")){
			Extractor ext = new Extractor(".", "example.Example", new String[] {"test"});
			List<DirectedGraph<Integer,DefaultEdge>> rpgList = ext.run();
			ext.quitVM();

			for (DirectedGraph<Integer,DefaultEdge> rpg : rpgList){
				try {
					Decoder decoder = new Decoder(rpg);
					int wNew = decoder.decodeRPGBento();
					System.out.println("The embedded watermark is possibly "+wNew);
				} catch (GraphStructureException e) {
					// ignore and go on to the next one
				}
			}
		}
	}
}
