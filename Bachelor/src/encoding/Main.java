package encoding;

import java.util.Arrays;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * This class is just for demonstrating en- and decoding.
 * @author Kaspar
 *
 */
public class Main {
	/**
	 * No creation of instances necessary or desirable.
	 */
	private Main(){}
	
	public static void main(String[] args){
		long w;
		if (args.length == 0){
			w = 5;
		} else {
			w = Integer.parseInt(args[0]);
		}
		System.out.println("Our number is: "+w);
		int[] sip = Encode.encodeWToSIP(w);
		System.out.println("The SIP is:\n"+Arrays.toString(sip));
		DirectedGraph<Integer, DefaultEdge> graph = Encode.encodeSIPtoRPG(sip);
		new GraphVisualizer<Integer>(graph);
		System.out.println("The graph is:\n"+graph.toString()+"\n");
		try {
			System.out.println("Now for the decoding of the graph...");
			Decoder<Integer> decoder = new Decoder<Integer>(graph);
			System.out.println("According to Bento, the number is: "+decoder.decodeRPGBento()+"\n");
			
			int[] sip2 = decoder.decodeRPGToSIP();
			System.out.println("The decoding algorithm of Chroni and Nikolopoulos gives the SIP:\n"+Arrays.toString(sip2));
			long w2 = decoder.decodeSIPToW(sip2);
			System.out.println("Which decodes to: "+w2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
