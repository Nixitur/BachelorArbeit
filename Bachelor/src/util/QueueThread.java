package util;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.*;

/**
 * This class provides the implementation of a thread which works its way through a <code>VirtualMachine</code>'s
 * events and waits for <code>MethodEntryEvent</code>s and <code>MethodExitEvent</code>s which it then processes.
 * @author Kaspar
 *
 */
public abstract class QueueThread extends Thread {

	private VirtualMachine vm;
	private EventRequestManager erm;
	
	public static final int ACTIVATE_METHOD_ENTRY_REQUEST = -1;
	public static final int ACTIVATE_METHOD_EXIT_REQUEST = 1;
	
	public boolean quitNow = false;
	
	/**
	 * Creates a new <code>QueueThread</code> which observes a certain <code>VirtualMachine</code> for <code>MethodEntryEvent</code>s whose
	 * methods are not in classes matching the pattern in <code>excludes</code>.
	 * @param vm The <code>VirtualMachine</code> to observe.
	 * @param excludes Strings of class patterns to exclude, e.g. Strings like "java.*" or "*.Foo".
	 */
	public QueueThread(VirtualMachine vm, String[] excludes, int flags) {
		this.vm = vm;
		this.erm = vm.eventRequestManager();
		
		if (flags <= 0){
			MethodEntryRequest mEnR = erm.createMethodEntryRequest();
			for (int i = 0; i < excludes.length; i++){
				mEnR.addClassExclusionFilter(excludes[i]);
			}
			mEnR.setSuspendPolicy(MethodEntryRequest.SUSPEND_ALL);
			mEnR.enable();
		}
		if (flags >= 0){
			MethodExitRequest mExR = erm.createMethodExitRequest();
			for (int i = 0; i < excludes.length; i++){
				mExR.addClassExclusionFilter(excludes[i]);
			}
			mExR.setSuspendPolicy(MethodExitRequest.SUSPEND_ALL);
			mExR.enable();
		}
		VMDeathRequest deathR = erm.createVMDeathRequest();
		deathR.setSuspendPolicy(VMDeathRequest.SUSPEND_ALL);
		deathR.enable();
	}
	
	@Override
	/**
	 * Runs this thread, waiting for events of class <code>MethodEntryEvent</code> or <code>MethodExitEvent</code>s.
	 */
	public void run() {
		EventQueue queue = vm.eventQueue();
		everything:
		while(true){
			try {
				EventSet eventSet = queue.remove();
				EventIterator it = eventSet.eventIterator();
				while (it.hasNext()){
					if (quitNow){
						break everything;
					}
					Event nextEvent = it.nextEvent();
					if (nextEvent instanceof VMDeathEvent){
						processDeath();
					} else {
						processMethodEvent(nextEvent);
					}
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
	public abstract void processDisconnected();

	/**
	 * Gets executed each time an event is received.
	 * @param e The event that is received.
	 */
	public abstract void processMethodEvent(Event e);
	
	/**
	 * Called if the target VM is dying. Most importantly, the VM is not yet disconnected at this point.
	 */
	public abstract void processDeath();
}
