package embedding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

import com.sun.jdi.Location;

import tracing.TracePoint;

/**
 * A thin container for a MethodGen that has to deal with TracePoints.
 * @author Kaspar
 *
 */
public class MethodContainer extends MethodGen  implements Constants{
	private static final long serialVersionUID = 1L;
	private HashMap<Location,Set<TracePoint>> locToTracePoint;
	private InstructionFactory _factory;
	private HashMap<Location,StackMapTableEntry> locToNextFrame;
	private Method method;
	private int _stackMapTableIndex;
	private ConstantPoolGen _cp;
	private StackMapTable _table = null;
	
	public MethodContainer(Method method, String className, ConstantPoolGen constantPool, InstructionFactory factory, int stackMapTableIndex){
		super(method, className, constantPool);
		this._stackMapTableIndex = stackMapTableIndex;
		this.method = method;
		this._cp = constantPool;
		locToTracePoint = new HashMap<Location,Set<TracePoint>>();
		locToNextFrame = new HashMap<Location,StackMapTableEntry>();
		this._factory = factory;
	}
	
	private void setUpFrames(){
		for (Attribute attribute : getCodeAttributes()){
			if (attribute instanceof StackMapTable){
				_table = (StackMapTable) attribute;
				break;
			}
		}
		if (_table == null){
			_table = new StackMapTable(_stackMapTableIndex, 2, new StackMapTableEntry[0], _cp.getFinalConstantPool());
			this.addCodeAttribute(_table);
		}
		List<Location> locList = new ArrayList<Location>(locToTracePoint.keySet());
		// TODO: Not O(n). Not sure if it could be done more efficiently.
		Collections.sort(locList);
		Location currentLoc = locList.get(0);
		int currentOffset = 0;
		int nextListIndex = 1;
		boolean isFirst = true;
		StackMapTableLoop:
		for (StackMapTableEntry entry : _table.getStackMapTable()) {
			// According to JVM Specification
			if (isFirst) {
				currentOffset = entry.getByteCodeOffsetDelta();
				isFirst = false;
			} else {
				currentOffset = currentOffset + entry.getByteCodeOffsetDelta() + 1;
			}
			while (currentOffset > currentLoc.codeIndex()){
				locToNextFrame.put(currentLoc, entry);
				if (nextListIndex < locList.size()){
					currentLoc = locList.get(nextListIndex);
					nextListIndex++;
				} else {
					break StackMapTableLoop;
				}
			}
		}
	}
	
	public void addTracePoint(TracePoint trace){
		Location loc = trace.getLoc();
		Set<TracePoint> traceSet = locToTracePoint.get(loc);
		if (traceSet == null){
			traceSet = new HashSet<TracePoint>();
			locToTracePoint.put(loc, traceSet);
		}
		traceSet.add(trace);
	}
	
	public void processTracePoints(){
		setUpFrames();
		InstructionList il = this.getInstructionList();
		il.setPositions(true);
		InstructionHandle markHandle;
		for (Location loc : locToTracePoint.keySet()){
			for (TracePoint trace : locToTracePoint.get(loc)){
				markHandle = il.findHandle((int) trace.getLoc().codeIndex());
				il.append(markHandle, InstructionConstants.NOP);
				StackMapTableEntry entry = locToNextFrame.get(loc);
				if (entry != null) {
					increaseOffsetDelta(entry,1);
				}
			}
		}
	}
	
	private void increaseOffsetDelta(StackMapTableEntry entry, int i){
		int type = entry.getFrameType();
		int delta = entry.getByteCodeOffsetDelta();
		int size = entry.getEntryByteSize();
		// This is valid for CHOP, APPEND and FULL_FRAME as they always make byte offset delta explicit and as such don't
		// change types if the byte offset delta is increased
		int newType = type;
		int newDelta = delta + i;
		
		if (((type >= Constants.SAME_FRAME) && (type <= Constants.SAME_FRAME_MAX)) 
				|| (type == Constants.SAME_FRAME_EXTENDED)){
			if (newDelta > Constants.SAME_FRAME_MAX){
				newType = Constants.SAME_FRAME_EXTENDED;
			} else {
				newType = newDelta;
			}
		} else if (((type >= Constants.SAME_LOCALS_1_STACK_ITEM_FRAME) && (type <= Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX)) 
				|| (type == Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)){
			if (newDelta > Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX){
				newType = Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
			} else {
				newType = newDelta;
			}
		}
		// Thankfully, byte offset delta is just ignored if it's not applicable
		entry.setFrameType(newType);
		entry.setByteCodeOffsetDelta(newDelta);
		int newSize = entry.getEntryByteSize();
		int difference = newSize - size;
		_table.setLength(_table.getLength() + difference);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodContainer other = (MethodContainer) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		return true;
	}
}
