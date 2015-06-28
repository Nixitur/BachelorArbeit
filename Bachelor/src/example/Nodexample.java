package example;

/**
 * An example class to see that my approach to graph-creating code works and to BCELize it.
 * @author Kaspar
 *
 */
public class Nodexample {
	public Nodexample e1,e2;
	public static Nodexample[] array;

	public static void testi(int val){
		Marker.mark(2*val+3);		
	}

	// Let's assume that the mark function was called with 9
	public static void testo(int val){
		if (val == 9){
			buildG0();
		} else {
			testN();
		}
	}
	
	public static void help(int i){}
	
	public static void testS(String val){
		Marker.mark(val+".");
	}
	
	public static void testoS(String val){
		if ((val+".").equals("lel.")){
			buildG0();
		}
	}
	
	public static void testN(){
		buildG0();
	}
	
	public static void buildG0(){
		if (array == null){
			array = new Nodexample[8];
			for (int i = 0; i < 8; i++){
				array[i] = new Nodexample();
			}
		}
		// nodes of G0
		Nodexample n1 = new Nodexample();
		Nodexample n0 = new Nodexample();
		Nodexample n_1 = new Nodexample();
		n1.e1 = n0;
		n0.e1 = n_1;
		
		array[0] = n0;
		array[1] = n1;
	}
	public static void buildG1(){
		if (array == null){
			array = new Nodexample[8];
			for (int i = 0; i < 8; i++){
				array[i] = new Nodexample();
			}
		}
		// firstOfPrevious
		Nodexample n1 = array[1];
		// nodes of G1
		Nodexample n4 = new Nodexample();
		Nodexample n3 = new Nodexample();
		Nodexample n2 = new Nodexample();		
		// inNeighbors of G1 not in G1
		// NONE because n1 already loaded
		
		// list edge to G0
		n2.e1 = n1;
		
		// list edges
		n4.e1 = n3;
		n3.e1 = n2;
		
		// tree edges to G1
		n1.e2 = n4;
		n2.e2 = n4;
		
		// n1, n2 finished so remove
		array[1] = new Nodexample();
		array[2] = new Nodexample();
		
		// n4,n3 still remaining
		array[3] = n3;
		array[4] = n4;
	}
	public static void buildG2(){
		if (array == null){
			array = new Nodexample[8];
			for (int i = 0; i < 8; i++){
				array[i] = new Nodexample();
			}
		}
		// firstOfPrevious
		Nodexample n4 = array[4];
		// nodes of G2
		Nodexample n7 = new Nodexample();
		Nodexample n6 = new Nodexample();
		Nodexample n5 = new Nodexample();
		// inNeighbors of G2 not in G2
		// n4 already declared
		Nodexample n0 = array[0];
		Nodexample n3 = array[3];
		
		// list edge to G1
		n5.e1 = n4;
		
		// list edges in G2
		n7.e1 = n6;
		n6.e1 = n5;
		
		// tree edges to G2
		n0.e2 = n6;
		n3.e2 = n7;
		n4.e2 = n6;
		n5.e2 = n7;
		n6.e2 = n7;
		
		// everything finished
		array[7] = n7;
		//array[7] = new Nodexample();
		array[6] = new Nodexample();
		array[5] = new Nodexample();
		array[4] = new Nodexample();
		array[0] = new Nodexample();
		array[3] = new Nodexample();
	}
	
	public static void main(String[] args){
		buildG0();
		buildG1();
		buildG2();
	}
}
