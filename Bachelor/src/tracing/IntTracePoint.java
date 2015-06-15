package tracing;

import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;

public class IntTracePoint extends TracePoint {
	
	private Location loc;
	private IntegerValue val;

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

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
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
