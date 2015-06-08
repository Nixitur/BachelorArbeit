package tracing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;

public class MarkTraceThread extends QueueThread {
	
	List<TracePoint> tracePoints;
	VirtualMachine vm;
	

	public MarkTraceThread(VirtualMachine vm, String[] excludes) {
		super(vm, excludes);
		this.vm = vm;
		tracePoints = new ArrayList<TracePoint>();
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
				Method caller = loc.method();
				System.out.println("---Entry "+caller.name()+"---");	
				System.out.println("---Line: "+loc.codeIndex()+"---");
				System.out.println(Arrays.toString(values.toArray()));
				Value val = null;
				if (!values.isEmpty()){
					val = values.get(0);
				}
				tracePoints.add(new TracePoint(caller, loc, val));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	void processDisconnected() {
		System.out.println(Arrays.toString(tracePoints.toArray()));
	}

}
