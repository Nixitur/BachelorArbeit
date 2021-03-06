package extraction;

import java.util.List;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;
import util.QueueThread;

public class ObjectConstructionThread extends QueueThread {
	
	private VirtualMachine vm;
	private RingBuffer<ObjectNode> constructedNodes;

	/**
	 * Creates a new ObjectConstructionThread that tracks construction of objects in a target VM with a limited ring buffer.
	 * @param vm The VM that this thread tracks.
	 * @param excludes A list of exclusion strings for classes that are not to be traced.
	 * @param size The size of the ring buffer.
	 */
	public ObjectConstructionThread(VirtualMachine vm, String[] excludes, int size) {
		super(vm, excludes, "object extraction");
		this.vm = vm;
		constructedNodes = new RingBuffer<ObjectNode>(size);
	}
	
	/**
	 * Creates a new ObjectConstructionThread that tracks construction of objects in a target VM with an unlimited FIFO buffer.
	 * @param vm The VM that this thread tracks.
	 * @param excludes A list of exclusion strings for classes that are not to be traced.
	 */
	public ObjectConstructionThread(VirtualMachine vm, String[] excludes){
		this(vm,excludes,RingBuffer.UNLIMITED_SIZE);
	}

	@Override
	public void processClassPrepareEvent(ClassPrepareEvent cPrE) {
		ReferenceType type = cPrE.referenceType();
		if (!ObjectNode.isValidType(type)){
			return;
		}
		List<Method> methods = type.methods();
		for (Method method : methods){
			if (method.isConstructor()){
				byte[] bytes = method.bytecodes();
				Location loc = method.locationOfCodeIndex(bytes.length-1);
				BreakpointRequest bPoR = erm.createBreakpointRequest(loc);
				bPoR.enable();
			}
		}
	}

	@Override
	public void processBreakpoint(BreakpointEvent bPoE) {
		try {
			StackFrame frame = bPoE.thread().frame(0);
			try {
				ObjectReference thiz = frame.thisObject();
				// Don't even save objects that are not valid nodes.
				// This might make it take longer, but requires less disabling of GC when edges are broken
				ObjectNode node = ObjectNode.checkIfValidRPGNode(thiz);
				if (node != null){
					constructedNodes.add(node);
					thiz.disableCollection();
				}
			} catch (com.sun.jdi.InternalException e2){
				// I'll be damned if I can figure out why this is happening, but if thisObject can't be gotten, just ignore the shit out of this object
			}
		} catch (IncompatibleThreadStateException e1) {
			e1.printStackTrace();
		}
	}
	
	public RingBuffer<ObjectNode> getNodes() throws Exception{
		if (this.isAlive()){
			throw new Exception("The ring buffer is getting changed right now, so you may not retrieve the constructed objects "
					+ "until this thread is no longer alive.");
		}
		return constructedNodes;
	}

	@Override
	public void processDisconnected() {
		// do absolutely nothing
	}

	@Override
	public void processDeath() {
		for (ObjectNode node : constructedNodes){
			node.updateChildren();
		}
		quitNow = true;
		// once the children have been set, the actual VM is not necessary anymore, so it just gets forcibly disconnected
		vm.dispose();
	}
}
