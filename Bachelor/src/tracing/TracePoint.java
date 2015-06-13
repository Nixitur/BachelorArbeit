package tracing;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.Value;

public class TracePoint {
	
	private Location loc;
	private Value val;

	public TracePoint(Location loc,Value val) {
		this.loc = loc;
		this.val = val;
	}
	
	public String methodSig() {
		Method method = loc.method();
		String className = method.declaringType().signature();
		// The declaring type is always "L" followed by the class name and ending with ";"
		className = className.substring(1, className.length()-1);
		// This signature has the same format as it is found as a "Methodref" in the constant pool in the class' Bytecode  
		String signature = className + "." + method.name() + ":" + method.signature();
		return signature;
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
		String signature = methodSig();
		String result = "("+signature+"|"+loc.codeIndex()+"|"+valToString+")";
		return result.toString();
	}
	
	public boolean sameLoc(TracePoint t){
		return (loc.equals(t.getLoc()));
	}

	public Location getLoc() {
		return loc;
	}

	public Value getVal() {
		return val;
	}
}
