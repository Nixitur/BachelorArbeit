package tracing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;

public class MarkTraceThread extends QueueThread {
	
	// We use LinkedHashSet because we do not want duplicate TracePoints, but still want an order
	// I declare it as such because the iteration order being the insertion order is actually important
	LinkedHashSet<TracePoint> tracePoints;
	VirtualMachine vm;
	

	public MarkTraceThread(VirtualMachine vm, String[] excludes) {
		super(vm, excludes);
		this.vm = vm;
		tracePoints = new LinkedHashSet<TracePoint>();
	}

	@Override
	void processEvent(Event event) {
		if (event instanceof MethodEntryEvent){
			MethodEntryEvent mEnE = (MethodEntryEvent) event;
			Method method = mEnE.method();
			if (!method.name().equals("mark")){
				return;
			}
			try {
				StackFrame thisFrame = mEnE.thread().frame(0);
				StackFrame callerFrame = mEnE.thread().frame(1);			
				Location loc = callerFrame.location();
				List<Value> values = thisFrame.getArgumentValues();
				Value val = null;
				if (!values.isEmpty()){
					val = values.get(0);
				} else if (values.size() > 1){
					throw new UnsupportedArgumentException("Calls to mark may only either "
							+ "have no argument or one argument of type int or String");
				}
				TracePoint tracePoint;
				if (val == null){
					tracePoint = new NoArgTracePoint(loc);
				} else if (val instanceof IntegerValue){
					tracePoint = new IntTracePoint(loc,(IntegerValue) val);
				} else if (val instanceof StringReference){
					tracePoint = new StringTracePoint(loc,(StringReference) val);
				} else {
					throw new UnsupportedArgumentException("Calls to mark may only either "
							+ "have no argument or one argument of type int or String");
				}
				System.out.println("---Entry "+loc.method().name()+"---");	
				System.out.println("---Line: "+loc.codeIndex()+"---");
				System.out.println(Arrays.toString(values.toArray()));
				tracePoints.add(tracePoint);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	void processDisconnected() {
		//System.out.println(Arrays.toString(tracePoints.toArray()));
	}
	
	public List<TracePoint> getTracePoints(){
		return new ArrayList<TracePoint>(tracePoints);
	}

}
