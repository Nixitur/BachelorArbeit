package mypackage;

import java.util.Stack;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class Encode {
	
	/**
	 * Construct the bitonic permutation from a number <code>w</code>
	 * @param w Any natural number
	 * @return The bitonic permutation of <code>w</code> as per the algorithm of Chroni and Nikolopoulos.
	 *         It is 0-indexed instead of 1-indexed to make calculations simpler.
	 */
	private static int[] encodeWToBitonic(int w){
		int[] flipB = Tools.bitFlip(Tools.bitrep(w));
		int n = flipB.length;
		int n2 = n * 2 + 1;
		int[] b = new int[n2];
		int noOfZeroes = 0;
		// Construct B*
		for (int i = 0; i < n2; i++){
			if (i<n){
				b[i] = 1;
			} else if(i < n2-1){
				b[i] = flipB[i-n];
			} else {
				b[i] = 0;
			}
			if (b[i] == 0){
				noOfZeroes++;
			}
		}
		int[] x = new int[noOfZeroes];
		int xIndex = 0;
		int[] y = new int[n2 - noOfZeroes];
		int yIndex = y.length-1;
		// Construct X and Y, arrays of indices of 0 and 1, respectively
		// Y is in reverse order
		for (int i = 0; i < n2; i++){
			if (b[i] == 0){
				x[xIndex] = i;
				xIndex++;
			} else {
				y[yIndex] = i;
				yIndex--;
			}
		}
		int[] bitonPerm = Tools.concatenate(x, y);
		return bitonPerm;
	}
	
	/**
	 * Calculates the Max-Didomination-edges for a self-inverting permutation
	 * @param sip A self-inverting permutation of odd length
	 * @return Array of Max-Didomination-edges as per the algorithm of Chroni and Nikolopoulos.
	 *         It is 0-indexed instead of 1-indexed to make calculations simpler.
	 */
	private static int[] calculateMaxDidom(int[] sip){
		int n = sip.length;
		int[] p = new int[n];
		Stack<Integer> s = new Stack<Integer>();
		s.push(new Integer(n));
		for (int i = 0; i < n; i++){
			while(s.peek() < sip[i]){
				s.pop();
			}
			p[sip[i]] = s.peek();
			s.push(new Integer(sip[i]));
		}
		return p;
	}
	
	/**
	 * Encodes <code>w</code> as a self-inverting permutation
	 * @param w Any natural number
	 * @return The self-inverting permutation of <code>w</code> as per the algorithm of Chroni
	 *         and Nikolopoulos. It is 0-indexed instead of 1-indexed to make calculations simpler.
	 */
	private static int[] encodeWToSIP(int w){
		int[] bitonPerm = encodeWToBitonic(w);
		int n = bitonPerm.length;
		int i = 0;
		int j = n-1;
		int[][] cycles = new int[n/2+1][2];
		while (i < j){
			cycles[i] = new int[]{bitonPerm[i],bitonPerm[j]};
			i++;
			j--;
		}
		if (i == j){
			cycles[i] = new int[]{bitonPerm[i],bitonPerm[i]};
		}
		int[] sip = new int[n];
		for (int[] cycle : cycles){
			sip[cycle[0]] = cycle[1];
			sip[cycle[1]] = cycle[0];
		}
		return sip;
	}
	
	/**
	 * Encodes the self-inverting permutation <code>sip</code> as a reducible permutation graph.
	 * @param sip The SIP to encode.
	 * @return The reducible permutation graph of <code>sip</code> as per the algorithm of Chroni and
	 *         Nikolopoulos. It is 1-indexed, as in the paper.
	 */
	private static DirectedGraph<Integer,DefaultEdge> encodeSIPtoRPG(int[] sip){
		int[] p = calculateMaxDidom(sip);
		int n = sip.length;
		DirectedGraph<Integer,DefaultEdge> graph = new SimpleDirectedGraph<Integer,DefaultEdge>(DefaultEdge.class);
		
		// Create n+2 vertices from 0 to n+1
		for (int i = 0; i <= n+1; i++){
			graph.addVertex(new Integer(i));
		}
		// Create the list edges
		for (int i = n+1; i > 0; i--){
			graph.addEdge(new Integer(i), new Integer(i-1));
		}
		// Create the max-didomination-edges
		for (int i = 0; i < n; i++){
			graph.addEdge(new Integer(i+1), new Integer(p[i]+1));
		}
		return graph;
	}

	public static void main(String[] args) {
		int w = Integer.parseInt(args[0]);
		int[] sip = encodeWToSIP(w);
		DirectedGraph<Integer,DefaultEdge> graph = encodeSIPtoRPG(sip);
		//new GraphVisualizer(graph);
		System.out.println(graph.toString());
	}

}
