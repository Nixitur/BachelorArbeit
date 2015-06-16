package tracing;

import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;

/**
 * An implementation of <code>TracePoint</code> for argument values of type <code>int</code>.
 * @author Kaspar
 *
 */
public class IntTracePoint extends TracePoint {
	
	private Location loc;
	private IntegerValue val;

	/**
	 * Creates a new instance at code location <code>loc</code> with the argument value being a <code>int</code> that is mirrored
	 * as <code>IntegerValue</code>.
	 * @param loc the code location
	 * @param val the mirrored <code>int</code> value.
	 */
	public IntTracePoint(Location loc, IntegerValue val) {
		super(loc, val);
		this.loc = loc;
		this.val = val;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
		result = prime * result + ((val == null) ? 0 : val.hashCode());
		return result;
	}


	/**
	 * Compares the specified Object with this IntTracePoint for equality.
	 * @param obj The object with which to compare.
	 * @return <code>true</code> if the Object is an IntTracePoint, if it refers to the same code location and if the IntegerValue that it references
	 *   refers to an <code>int</code> value that is the same as this IntTracePoint's, <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		// This works because equals on IntegerValue is defined as equality of the mirrored value
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntTracePoint other = (IntTracePoint) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		if (val == null) {
			if (other.val != null)
				return false;
		} else if (!val.equals(other.val))
			return false;
		return true;
	}
}
