package tracing;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.*;

public abstract class QueueThread extends Thread {

	private VirtualMachine vm;
	private EventRequestManager erm;
	
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
	public void run() {
		EventQueue queue = vm.eventQueue();
		while(true){
			try {
				EventSet eventSet = queue.remove();
				EventIterator it = eventSet.eventIterator();
				while (it.hasNext()){
					processEvent(it.nextEvent());
					//vm.resume();
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
	
	abstract void processDisconnected();

	/**
	 * Gets executed each time an event is received.
	 * @param e The event that is received.
	 */
	abstract void processEvent(Event e);
}
