package extraction;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodExitEvent;

import util.QueueThread;

public class ObjectConstructionThread extends QueueThread {
	
	private VirtualMachine vm;
	private RingBuffer<ObjectReference> constructedObjects;
	private static final int BUFFER_SIZE = 1000;

	public ObjectConstructionThread(VirtualMachine vm, String[] excludes) {
		super(vm, excludes,ACTIVATE_METHOD_EXIT_REQUEST);
		this.vm = vm;
		constructedObjects = new RingBuffer<ObjectReference>(BUFFER_SIZE);
	}

	@Override
	public void processMethodEvent(Event e) {
		if (!(e instanceof MethodExitEvent)){
			return;
		}
		MethodExitEvent mExE = ((MethodExitEvent) e);
		Method method = mExE.method();
		if (!(method.isConstructor())){
			return;
		}
		try {
			ObjectReference thiz = mExE.thread().frame(0).thisObject();
			// Don't even save objects that are not valid nodes.
			// This might make it take longer, but requires less disabling of GC when edges are broken
			ObjectNode node = ObjectNode.checkIfValidRPGNode(thiz);
			if (node != null){
				constructedObjects.add(thiz);
			}
		} catch (IncompatibleThreadStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public RingBuffer<ObjectReference> getObjects() throws Exception{
		if (this.isAlive()){
			throw new Exception("The ring buffer is getting changed right now, so you may not retrieve the constructed objects "
					+ "until this thread is no longer alive.");
		}
		return constructedObjects;
	}

	@Override
	public void processDisconnected() {
		// do absolutely nothing
	}

	@Override
	public void processDeath() {
		quitNow = true;
		vm.suspend();
	}
}
