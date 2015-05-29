package mypackage;

public class Tools {

	/**
	 * Creates the bit-representation of the natural number <code>w</code>
	 * @param w The number to encode in base 2
	 * @return The base-2 representation of <code>w</code>
	 */
	static int[] bitrep(int w){
		int n = (int) Math.ceil(Math.log((double)w) / Math.log(2.0));
		int rest = w;
		int[] result = new int[n];
		for (int i = n-1;i>=0;i--){
			result[i] = rest % 2;
			rest = rest / 2;
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

}
