package example;

import java.io.IOException;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import util.ConfigParser;
import util.TimeKeeper;
import encoding.Decoder;
import extraction.Extractor;

public class Main {

	public static void main(String[] args) throws IOException{
		if ((args.length < 2) || (!(args[0].equals("encode")) && !(args[0].equals("decode"))) ){
			System.out.println("Please enter either \"encode\" to encode and embed the watermark or \"decode\" to extract and decode "
					+ "the watermark as the first argument and the config file name as the second argument.");
			return;
		}
		TimeKeeper time;
		String fileName = args[1];
		ConfigParser parser = new ConfigParser(fileName);
		String classPath = parser.classPath();
		String mainClass = parser.mainClass();
		String arguments = parser.arguments();
		if (args[0].equals("encode")){
			long w = parser.encodingNumber();
			String fullClassName = parser.watermarkClass();
			time = new TimeKeeper("encoding");
			DirectedGraph<Integer, DefaultEdge> graph = encoding.Encode.encodeWToRPG(w);
			time.stop();

			embedding.Embedder embedder = new embedding.Embedder(classPath, mainClass, arguments, parser.markMethod());
			int noOfTracePoints = embedder.run();
			
			if (noOfTracePoints == 0){
				System.out.println("No trace points hit. Try again.");
				return;
			}
			int[] flipEdgeNumbers = parser.flipEdgeNumbers();
			int deleteEdgeType = parser.deleteEdgeType();
			int deleteEdgeNumber = parser.deleteEdgeNumber();
			
			time = new TimeKeeper("wmark creation");
			embedding.WatermarkCreator wmark = new embedding.WatermarkCreator(fullClassName, graph, noOfTracePoints);
			int noOfBuildMethods = wmark.create();
			for (int num : flipEdgeNumbers){
				try {
					wmark.edgeFlip(num);
				} catch (IllegalStateException e) {
					// if this vertex can't be edge-flipped, just ignore it
				}
			}
			if (deleteEdgeNumber > 0){
				if (deleteEdgeType == ConfigParser.LIST_EDGE){
					wmark.deleteListEdge(deleteEdgeNumber);
				} else if (deleteEdgeType == ConfigParser.TREE_EDGE){
					wmark.deleteTreeEdge(deleteEdgeNumber);
				}
			}
			time.stop();
			try {
				wmark.dump(classPath);
			} catch (IllegalStateException e) {
				// Can never happen because it creates first.
				e.printStackTrace();
			}
			String wmarkClassName = wmark.getClassName();
			
			embedder.dump(wmarkClassName, noOfBuildMethods);
		}
		if (args[0].equals("decode")){
			Extractor ext = new Extractor(classPath, mainClass, arguments);
			Set<Set<Integer>> rootChildrenSets = ext.run();
			//ext.quitVM();

			time = new TimeKeeper("decoding");
			for (Set<Integer> rootChildren : rootChildrenSets){
				long wNew = Decoder.decodeRootChildren(rootChildren);
				System.out.println("The embedded watermark is possibly "+wNew);
			}
			time.stop();
		}
		TimeKeeper.dump();
	}
}
