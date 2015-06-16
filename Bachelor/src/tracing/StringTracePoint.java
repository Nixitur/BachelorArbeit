package tracing;

import com.sun.jdi.Location;
import com.sun.jdi.StringReference;

/**
 * An implementation of <code>TracePoint</code> for argument values of type <code>java.lang.String</code>.
 * @author Kaspar
 *
 */
public class StringTracePoint extends TracePoint {
	
	private Location loc;
	private String stringValue;

	/**
	 * Creates a new instance at code location <code>loc</code> with the argument value being a String that is mirrored as <code>StringReference</code>.
	 * @param loc the code location
	 * @param val the mirrored String value
	 */
	public StringTracePoint(Location loc, StringReference val) {
		super(loc, val);
		stringValue = val.value();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
		result = prime * result
				+ ((stringValue == null) ? 0 : stringValue.hashCode());
		return result;
	}
	
	@Override
	/**
	 * Compares the specified Object with this StringTracePoint for equality.
	 * @param obj The object with which to compare.
	 * @return <code>true</code> if the Object is a StringTracePoint, if it refers to the same code location and if the StringReference that it
	 *   references refers to a <code>String</code> that is the same as this StringTracePoint's, <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		// This works because I specifically compare the mirrored Strings
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringTracePoint other = (StringTracePoint) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		if (stringValue == null) {
			if (other.stringValue != null)
				return false;
		} else if (!stringValue.equals(other.stringValue))
			return false;
		return true;
	}

}
