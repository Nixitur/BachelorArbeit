package tracing;

import com.sun.jdi.Location;
import com.sun.jdi.Value;

public abstract class TracePoint {
	
	private Location loc;
	private Value val;

	public TracePoint(Location loc,Value val) {
		this.loc = loc;
		this.val = val;
	}
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		String valToString = "NONE";
		if (!(val == null)){
			valToString = val.toString();
		}
		String signature = embedding.Tools.methodSig(loc);
		String result = "("+signature+"|"+loc.codeIndex()+"|"+valToString+")";
		return result.toString();
	}

	public Location getLoc() {
		return loc;
	}

	public Value getVal() {
		return val;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public abstract int hashCode();

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public abstract boolean equals(Object obj);
	
	
}
