package tracing;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.*;

/**
 * This class provides the implementation of a thread which works its way through a <code>VirtualMachine</code>'s
 * events and waits for <code>MethodEntryEvent</code>s which it then processes.
 * @author Kaspar
 *
 */
public abstract class QueueThread extends Thread {

	private VirtualMachine vm;
	private EventRequestManager erm;
	
	/**
	 * Creates a new <code>QueueThread</code> which observes a certain <code>VirtualMachine</code> for <code>MethodEntryEvent</code>s whose
	 * methods are not in classes matching the pattern in <code>excludes</code>.
	 * @param vm The <code>VirtualMachine</code> to observe.
	 * @param excludes Strings of class patterns to exclude, e.g. Strings like "java.*" or "*.Foo". 
	 */
	public QueueThread(VirtualMachine vm, String[] excludes) {
		this.vm = vm;
		this.erm = vm.eventRequestManager();
		MethodEntryRequest mEnR = erm.createMethodEntryRequest();
		MethodExitRequest mExR = erm.createMethodExitRequest();
		for (int i = 0; i < excludes.length; i++){
			mEnR.addClassExclusionFilter(excludes[i]);
			mExR.addClassExclusionFilter(excludes[i]);
		}
		mEnR.setSuspendPolicy(MethodEntryRequest.SUSPEND_ALL);
		mExR.setSuspendPolicy(MethodExitRequest.SUSPEND_ALL);
		mEnR.enable();
		mExR.enable();
	}
	
	@Override
	/**
	 * Runs this thread, waiting for events of class <code>MethodEntryEvent</code>.
	 */
	public void run() {
		EventQueue queue = vm.eventQueue();
		while(true){
			try {
				EventSet eventSet = queue.remove();
				EventIterator it = eventSet.eventIterator();
				while (it.hasNext()){
					processEvent(it.nextEvent());
				}
				eventSet.resume();
			} catch (InterruptedException e) {
				// Ignore
			} catch (VMDisconnectedException e){
				processDisconnected();
				break;
			}
		}
	}
	
	/**
	 * Called if the target VM disconnects.
	 */
	abstract void processDisconnected();

	/**
	 * Gets executed each time an event is received.
	 * @param e The event that is received.
	 */
	abstract void processEvent(Event e);
}
