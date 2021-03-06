package util;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
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
	protected EventRequestManager erm;
	private String name;
	
	public boolean quitNow = false;
	
	/**
	 * Creates a new <code>QueueThread</code> which observes a certain <code>VirtualMachine</code> for <code>MethodEntryEvent</code>s whose
	 * methods are not in classes matching the pattern in <code>excludes</code>.
	 * @param vm The <code>VirtualMachine</code> to observe.
	 * @param excludes Strings of class patterns to exclude, e.g. Strings like "java.*" or "*.Foo".
	 */
	public QueueThread(VirtualMachine vm, String[] excludes, String name) {
		this.name = name;
		this.vm = vm;
		this.erm = vm.eventRequestManager();
		
		ClassPrepareRequest cPrR = erm.createClassPrepareRequest();
		for (int i = 0; i < excludes.length; i++){
			cPrR.addClassExclusionFilter(excludes[i]);
		}
		cPrR.setSuspendPolicy(ClassPrepareRequest.SUSPEND_ALL);
		cPrR.enable();
		
		VMDeathRequest deathR = erm.createVMDeathRequest();
		deathR.setSuspendPolicy(VMDeathRequest.SUSPEND_ALL);
		deathR.enable();
	}
	
	@Override
	/**
	 * Runs this thread, waiting for events of class <code>MethodEntryEvent</code> or <code>MethodExitEvent</code>s.
	 */
	public void run() {
		TimeKeeper time = new TimeKeeper(name);
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
					} else if (nextEvent instanceof ClassPrepareEvent){
						ClassPrepareEvent cPrE = (ClassPrepareEvent) nextEvent;
						processClassPrepareEvent(cPrE);
					} else if (nextEvent instanceof BreakpointEvent){
						BreakpointEvent bPoE = (BreakpointEvent) nextEvent;
						processBreakpoint(bPoE);
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
		time.stop();
	}
	
	/**
	 * Called if the target VM disconnects.
	 */
	public abstract void processDisconnected();
	
	/**
	 * Gets executed each time a class is prepared.
	 * @param cPrE The ClassPrepareEvent
	 */
	public abstract void processClassPrepareEvent(ClassPrepareEvent cPrE);
	
	/**
	 * Gets executed each time a breakpoint is reached.
	 * @param bPoE The BreakpointEvent
	 */
	public abstract void processBreakpoint(BreakpointEvent bPoE);
	
	/**
	 * Called if the target VM is dying. Most importantly, the VM is not yet disconnected at this point.
	 */
	public abstract void processDeath();
}
