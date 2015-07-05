package extraction;

import java.util.List;
import java.util.Map;

import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodExitEvent;

import util.QueueThread;

public class ObjectConstructionThread extends QueueThread {
	
	private RingBuffer<ObjectReference> constructedObjects;
	private static final int BUFFER_SIZE = 1000;

	public ObjectConstructionThread(VirtualMachine vm, String[] excludes) {
		super(vm, excludes,ACTIVATE_METHOD_EXIT_REQUEST);
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
			constructedObjects.add(thiz);
		} catch (IncompatibleThreadStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public void processDisconnected() {
		// do absolutely nothing
	}

	@Override
	public void processDeath() {
		for (ObjectReference or : constructedObjects){
			List<Field> fields = or.referenceType().allFields();
			Map<Field, Value> values = or.getValues(fields);
			values.getClass();
		}
	}
}
