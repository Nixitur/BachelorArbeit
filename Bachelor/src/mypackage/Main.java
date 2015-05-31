package mypackage;

import java.util.Arrays;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class Main {

	public Main() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args){
		int w;
		if (args.length == 0){
			w = 5;
		} else {
			w = Integer.parseInt(args[0]);
		}
		int[] sip = Encode.encodeWToSIP(w);
		System.out.println(Arrays.toString(sip));
		DirectedGraph<Integer, DefaultEdge> graph = Encode.encodeSIPtoRPG(sip);
		//new GraphVisualizer(graph);
		System.out.println(graph.toString());
		try {
			Decoder decoder = new Decoder(graph);
			List<Integer> derp = decoder.decodeRPGToSIP();
			System.out.println(Arrays.toString(derp.toArray()));
			System.out.println(""+decoder.decodeRPGBento());
		} catch (GraphStructureException e) {
			e.printStackTrace();
		}
	}

}
