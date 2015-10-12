package encoding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Tools {	
	/**
	 * No creation of instances necessary or desirable.
	 */
	private Tools(){}

	/**
	 * Creates the bit-representation of the natural number <code>w</code>
	 * @param w The number to encode in base 2
	 * @return The base-2 representation of <code>w</code>
	 */
	static int[] bitrep(long w){
		// It's IMPORTANT to do floor()+1, not ceil() because otherwise, stuff like 52.00000000001 will get ceil'd to 52
		int n = (int) Math.floor(Math.log((double)w) / Math.log(2.0))+1;
		long rest = w;
		int[] result = new int[n];
		for (int i = n-1;i>=0;i--){
			result[i] = (int) (rest % 2);
			rest = rest / 2;
		}		
		return result;
	}
	
	/**
	 * Calculates the number represented by the base-2 representation <code>b</code>.
	 * @param b A base-2 representation of a number, i.e. it only consists of 0 and 1.
	 * @return The number represented by <code>b</code>.
	 */
	static long bitrepToW(int[] b){
		long result = 0;
		int length = b.length;
		for (int i = 0; i < length; i++){
			if (b[i] == 1){
				result = (long) (result + (Math.pow(2, length-i-1)));
			}
		}
		return result;
	}
	
	static List<Integer> fillWithNegative(int size){
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < size; i++){
			result.add(new Integer(-1));
		}
		return result;
	}

	/**
	 * Flips the bits in a bit-representation
	 * @param b The array to be flipped
	 * @return <code>b</code> with all <code>0</code>s replaced by <code>1</code>
	 *         and every other number replaced by <code>0</code>
	 */
	static int[] bitFlip(int[] b){
		int[] result = new int[b.length];
		for (int i=0;i<b.length;i++){
			if (b[i] == 0){
				result[i] = 1;
			} else {
				result[i] = 0;
			}
		}
		return result;
	}

	/**
	 * Concatenates two <code>int[]</code>-arrays
	 * @param x The first array
	 * @param y The second array
	 * @return The concatenation of the first and the second array
	 */
	static int[] concatenate(int[] x, int[] y){
		int xLen = x.length;
		int yLen = y.length;
		int[] z = new int[xLen + yLen];
		System.arraycopy(x,0,z,0,xLen);
		System.arraycopy(y,0,z,xLen,yLen);
		return z;
	}
	
	/**
	 * Simply finds the largest element in a collection.
	 * @param <V> Type of the elements in <code>col</code>
	 * @param col The collection to search through.
	 * @return The largest element in <code>col</code>
	 */
	static <V extends Comparable<? super V>> V findLargest(Collection<V> col){
		V largest = col.iterator().next();
		for (V element : col){
			if (element.compareTo(largest) > 0){
				largest = element;
			}
		}
		return largest;
	}
	
	
	static List<Integer> arrayToList(int[] array, int startIndex, int endIndex){
		List<Integer> result = new ArrayList<Integer>();
		for (int i = startIndex; i < endIndex; i++){
			result.add(new Integer(array[i]));
		}
		return result;
	}

	/**
	 * Returns the hamiltonian path of an RPG with <code>n</code> vertices. It's simply a list from n-2 to -1.
	 * @param n The number of vertices.
	 * @return A list of <code>Integer</code> objects going from n-2 to 1.
	 */
	public static List<Integer> hamiltonPath(int n){
		List<Integer> result = new ArrayList<Integer>();
		for (int i = n-2; i >= -1; i--){
			result.add(new Integer(i));
		}
		return result;
	}

}
