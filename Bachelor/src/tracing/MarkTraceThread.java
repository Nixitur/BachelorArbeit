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
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;

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
	private String markMethodName;
	
	
	/**
	 * Creates a new instance which observes a certain <code>VirtualMachine</code> for <code>MethodEntryEvent</code>s whose
	 * methods are not in classes matching the pattern in <code>excludes</code>.
	 * @param vm The <code>VirtualMachine</code> to observe.
	 * @param excludes Strings of class patterns to exclude, e.g. Strings like "java.*" or "*.Foo".
	 * @param markMethodName The full name of the mark method. This needs to contain the package as well as the class, so
	 *  for example "mark" would not be correct, but "myPackage.Marker.mark" would be.
	 */
	public MarkTraceThread(VirtualMachine vm, String[] excludes, String markMethodName) {
		super(vm, excludes, ACTIVATE_METHOD_ENTRY_REQUEST, "tracing");
		this.markMethodName = markMethodName;
		tracePoints = new LinkedHashSet<TracePoint>();
		toBeDeleted = new HashSet<TracePoint>();
	}

	@Override
	/**
	 * Processes a received event. If <code>event</code> is a <code>MethodEntryEvent</code> to a mark method (defined as having the name
	 * <code>markMethodName</code>) with the correct arguments, it adds a <code>TracePoint</code> to <code>tracePoints</code> containing
	 * the location and value of the call. 
	 * @param event The event that is received.
	 * @throws UnsupportedArgumentException if the call to the mark method has too many arguments or arguments of the wrong type.
	 */
	public void processMethodEvent(Event event) {
		if (!(event instanceof MethodEntryEvent)){
			return;
		}
		MethodEntryEvent mEnE = ((MethodEntryEvent) event);
		Method method = mEnE.method();
		String fullClassName = method.declaringType().name();
		String fullMethodName = fullClassName + "." + method.name();
		if (!fullMethodName.equals(markMethodName)){
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
