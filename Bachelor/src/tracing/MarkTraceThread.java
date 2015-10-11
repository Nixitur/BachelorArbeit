package tracing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import util.QueueThread;
import util.UnsupportedArgumentException;

import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;

/**
 * This <code>QueueThread</code> specifically waits for calls to a mark method in the target VM and stores
 * the location and arguments of those calls.
 * @author Kaspar
 *
 */
public class MarkTraceThread extends QueueThread {
	
	// We use LinkedHashSet because we do not want duplicate TracePoints, but still want an order
	// I declare it as such because the iteration order being the insertion order is actually important
	private LinkedHashSet<TracePoint> tracePoints;
	private Set<TracePoint> toBeDeleted;
	private String markClassName;
	private String markMethodName;
	
	
	/**
	 * Creates a new instance which observes a certain <code>VirtualMachine</code> for <code>MethodEntryEvent</code>s whose
	 * methods are not in classes matching the pattern in <code>excludes</code>.
	 * @param vm The <code>VirtualMachine</code> to observe.
	 * @param excludes Strings of class patterns to exclude, e.g. Strings like "java.*" or "*.Foo".
	 * @param fullMarkMethodName The full name of the mark method. This needs to contain the package as well as the class, so
	 *  for example "mark" would not be correct, but "myPackage.Marker.mark" would be.
	 */
	public MarkTraceThread(VirtualMachine vm, String[] excludes, String fullMarkMethodName) {
		super(vm, excludes, "tracing");
		int i = fullMarkMethodName.lastIndexOf('.');
		markClassName = fullMarkMethodName.substring(0, i);
		markMethodName = fullMarkMethodName.substring(i+1, fullMarkMethodName.length());
		tracePoints = new LinkedHashSet<TracePoint>();
		toBeDeleted = new HashSet<TracePoint>();
	}
	
	@Override
	public void processClassPrepareEvent(ClassPrepareEvent cPrE) {
		ReferenceType type = cPrE.referenceType();
		if (!type.name().equals(markClassName)){
			return;
		}
		// The class is loaded, get all its methods
		List<Method> methods = type.methods();
		for (Method method : methods){
			if (method.name().equals(markMethodName)){				
				BreakpointRequest bPoR = erm.createBreakpointRequest(method.location());
				bPoR.enable();
			}
		}
	}
	
	@Override
	public void processBreakpoint(BreakpointEvent bPoE){
		try {
			StackFrame thisFrame = bPoE.thread().frame(0);
			StackFrame callerFrame = bPoE.thread().frame(1);			
			Location loc = callerFrame.location();
			List<Value> values = thisFrame.getArgumentValues();
			Value val = null;
			if (!values.isEmpty()){
				val = values.get(0);
			} else if (values.size() > 1){
				throw new UnsupportedArgumentException("Too many arguments");
			}
			TracePoint tracePoint;
			if (val == null){
				tracePoint = new NoArgTracePoint(loc);
			} else if (val instanceof IntegerValue){
				tracePoint = new IntTracePoint(loc,(IntegerValue) val);
			} else if (val instanceof StringReference){
				tracePoint = new StringTracePoint(loc,(StringReference) val);
			} else {
				throw new UnsupportedArgumentException("Argument is not of type int or java.lang.String");
			}
			System.out.println("---Entry "+loc.method().name()+"---");	
			System.out.println("---Line: "+loc.codeIndex()+"---");
			System.out.println(Arrays.toString(values.toArray()));
			if (tracePoints.contains(tracePoint)){
				toBeDeleted.add(tracePoint);
			} else {
				tracePoints.add(tracePoint);
			}				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	/**
	 * Called if the target VM is disconnected. If this happens, duplicates are removed from <code>tracePoints</code> and they are enumerated
	 * in the correct order.
	 */
	public void processDisconnected() {
		removeDuplicates();
		setIndices();
	}
	
	/**
	 * Returns the LinkedHashSet of trace points. This set is backed by this instance, so you may not call this
	 * method until <code>this.isAlive()</code> is <code>false</code>.
	 * @return The set of TracePoints hit in the execution of the target VM. The iterator on this set returns the
	 *   TracePoints in the order in which they are hit.
	 * @throws Exception If this thread is still alive.
	 */
	public LinkedHashSet<TracePoint> getTracePoints() throws Exception{
		if (this.isAlive()){
			throw new Exception("The set is getting changed right now, so you may not retrieve the TracePoints "
					+ "until this thread is no longer alive.");
		}
		return tracePoints;
	}
	
	/**
	 * For each <code>TracePoint</code> in <code>tracePoints</code> that was reached multiple times during the course of this thread's
	 * lifetime, this <code>TracePoint</code> is removed from <code>tracePoints</code>.
	 */
	private void removeDuplicates(){
		tracePoints.removeAll(toBeDeleted);
	}
	
	/**
	 * Sets the correct indices for each TracePoint in the order in which they appear in tracePoints.
	 */
	private void setIndices(){
		int index = 0;
		for (TracePoint trace : tracePoints){
			trace.setIndex(index);
			index++;
		}
	}

	/**
	 * Called if the VM is dying. In this case, absolutely nothing happens.
	 */
	@Override
	public void processDeath() {
		// do absolutely nothing
	}
}
