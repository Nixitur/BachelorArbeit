package encoding;

/**
 * Instances of this class are a representation of a 1- or 2-cycle in a permutation.
 * @author Kaspar
 *
 */
public class Cycle implements Comparable<Cycle>{
	/**
	 * The first element of the cycle in in a <code>(x,y)</code> representation where x &le; y
	 */
	public final int x;
	/**
	 * The second element of the cycle in in a <code>(x,y)</code> representation where x &le; y
	 */
	public final int y;

	/**
	 * Constructs a cycle from two elements. The arguments need not be in any particular order nor need they be distinct.
	 * @param c1 One element of the cycle.
	 * @param c2 The other element of the cycle.
	 */
	public Cycle(int c1, int c2) {
		if (c1 > c2){
			this.x = c2;
			this.y = c1;
		} else {
			this.x = c1;
			this.y = c2;
		}
	}

	/**
	 * Returns 0 if the first element in this cycle is equal to the one in the argument. Is negative if the
	 * first element in this cycle is smaller than the one in the argument. Is positive if the first element
	 * in this cycle is larger than the one in the argument.
	 */
	@Override
	public int compareTo(Cycle arg0) {
		return (this.x - arg0.x);
	}
}
