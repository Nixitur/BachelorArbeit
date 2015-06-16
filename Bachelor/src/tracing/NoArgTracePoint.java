package tracing;

import com.sun.jdi.Location;

/**
 * An implementation of <code>TracePoint</code> for a call to a mark method without arguments. 
 * @author Kaspar
 *
 */
public class NoArgTracePoint extends TracePoint {
	
	Location loc;

	/**
	 * Creates a new NoArgTracePoint.
	 * @param loc The code location that is hit during tracing. 
	 */
	public NoArgTracePoint(Location loc) {
		super(loc, null);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
		return result;
	}

	/**
	 * Compares the specified Object with this NoArgTracePoint for equality.
	 * @param obj The object with which to compare.
	 * @return <code>true</code> if the Object is a NoArgTracePoint and if it refers to the same code location, <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NoArgTracePoint other = (NoArgTracePoint) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		return true;
	}

}
