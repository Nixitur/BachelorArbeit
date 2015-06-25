package tracing;

import com.sun.jdi.Location;
import com.sun.jdi.Value;

/**
 * This class is an implementation of the idea of "trace points" in "Dynamic Graph-Based Software Fingerprinting" by Collberg, Thomborson and
 * Townsend. They contain a <code>Location</code> which describes a location in a class' code where a mark method was called and a <code>Value</code>
 * which is the argument of that method call.
 * @author Kaspar
 *
 */
public abstract class TracePoint implements Comparable<TracePoint>{
	
	private final Location loc;
	private final Value val;
	private int index;

	/**
	 * Creates a new instance with the code location <code>Location</code> and the argument value <code>Value</code>.
	 * @param loc the code location
	 * @param val the argument value. If this is <code>null</code>, it means that no argument is passed.
	 */
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
		String result = index+":("+signature+"|"+loc.codeIndex()+"|"+valToString+")";
		return result.toString();
	}
	
	public int compareTo(TracePoint t){
		return (int) (t.loc.codeIndex() - loc.codeIndex());
	}

	/**
	 * Returns the code location of this instance.
	 * @return the <code>Location</code> that is referenced in this instance.
	 */
	public Location getLoc() {
		return loc;
	}

	/**
	 * Returns the value of this instance.
	 * @return the <code>Value</code> that is referenced in this instance.
	 */
	public Value getVal() {
		return val;
	}
	
	/**
	 * Sets the index field.
	 * @param index the index
	 */
	public void setIndex(int index){
		this.index = index;
	}
	
	/**
	 * Returns the index. This is for knowing which graph-building method to call.
	 * @return the index
	 */
	public int getIndex(){
		return index;
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
