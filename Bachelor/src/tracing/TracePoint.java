package tracing;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.Value;

public class TracePoint {
	
	private Method method;
	private Location loc;
	private Value val;

	public TracePoint(Method method,Location loc,Value val) {
		this.method = method;
		this.loc = loc;
		this.val = val;
	}
	
	@Override
	public String toString(){
		String valToString = "null";
		if (!(val == null)){
			valToString = val.toString();
		}
		String result = "("+valToString+"|"+loc.lineNumber()+"|"+method.name()+")";
		return result.toString();
	}
}
