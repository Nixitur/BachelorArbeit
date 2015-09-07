package example;

import java.io.IOException;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import util.ConfigParser;
import encoding.Decoder;
import extraction.Extractor;

public class Main {

	public static void main(String[] args) throws IOException{
		if ((args.length < 2) || (!(args[0].equals("encode")) && !(args[0].equals("decode"))) ){
			System.out.println("Please enter either \"encode\" to encode and embed the watermark or \"decode\" to extract and decode "
					+ "the watermark as the first argument and the config file name as the second argument.");
			return;
		}
		String fileName = args[1];
		ConfigParser parser = new ConfigParser(fileName);
		String classPath = parser.classPath();
		String mainClass = parser.mainClass();
		String[] arguments = parser.arguments();
		if (args[0].equals("encode")){
			int w = parser.encodingNumber();
			String packageName = parser.watermarkPackage();
			int[] sip = encoding.Encode.encodeWToSIP(w);
			DirectedGraph<Integer, DefaultEdge> graph = encoding.Encode.encodeSIPtoRPG(sip);

			embedding.Embedder embedder = new embedding.Embedder(classPath, mainClass, arguments, parser.markMethod());
			int noOfTracePoints = embedder.run();

			embedding.WatermarkCreator wmark = new embedding.WatermarkCreator(packageName, graph, noOfTracePoints);
			int noOfBuildMethods = wmark.create();
			int edgeType = parser.edgeType();
			int edgeNumber = parser.edgeNumber();
			if (edgeNumber > 0){
				if (edgeType == ConfigParser.LIST_EDGE){
					wmark.deleteListEdge(edgeNumber);
				} else if (edgeType == ConfigParser.TREE_EDGE){
					wmark.deleteTreeEdge(edgeNumber);
				}
			}
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
			Extractor ext = new Extractor(classPath, mainClass, arguments);
			Set<Set<Integer>> rootChildrenSets = ext.run();
			ext.quitVM();

			for (Set<Integer> rootChildren : rootChildrenSets){
				int wNew = Decoder.decodeRootChildren(rootChildren);
				System.out.println("The embedded watermark is possibly "+wNew);
			}
		}
	}
}
