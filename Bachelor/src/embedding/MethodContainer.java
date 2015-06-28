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
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.PUSH;

import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

import tracing.*;

/**
 * A thin container for a MethodGen that has to deal with TracePoints.
 * @author Kaspar
 *
 */
public class MethodContainer extends MethodGen  implements Constants{
	private static final long serialVersionUID = 1L;
	private String _watermarkClassName;
	private HashMap<Location,Set<TracePoint>> locToTracePoint;
	private InstructionFactory _factory;
	// for knowing which frame to edit
	private HashMap<Location,StackMapTableEntry> locToNextFrame;
	// for knowing where to insert new frames if necessary
	private HashMap<Location,Integer> locToNextFrameIndex;
	private HashMap<StackMapTableEntry,Integer> totalBytecodeOffset;
	private Method method;
	private int _stackMapTableIndex;
	private ConstantPoolGen _cp;
	private StackMapTable _table = null;
	
	public MethodContainer(Method method, String className, ConstantPoolGen constantPool, InstructionFactory factory, 
			int stackMapTableIndex, String watermarkClassName){
		super(method, className, constantPool);
		_watermarkClassName = watermarkClassName;
		this._stackMapTableIndex = stackMapTableIndex;
		this.method = method;
		this._cp = constantPool;
		locToTracePoint = new HashMap<Location,Set<TracePoint>>();
		locToNextFrame = new HashMap<Location,StackMapTableEntry>();
		locToNextFrameIndex = new HashMap<Location,Integer>();
		totalBytecodeOffset = new HashMap<StackMapTableEntry,Integer>();
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
		totalBytecodeOffset = constructTotalBytecodeOffsets();
		List<Location> locList = new ArrayList<Location>(locToTracePoint.keySet());
		// TODO: Not O(n). Not sure if it could be done more efficiently.
		Collections.sort(locList);
		Location currentLoc = locList.get(0);
		int currentOffset = 0;
		int nextListIndex = 1;
		boolean isFirst = true;
		StackMapTableEntry[] entries = _table.getStackMapTable();
		StackMapTableEntry entry;
		StackMapTableLoop:
		for (int i = 0; i < entries.length; i++){
			entry = entries[i];
			// According to JVM Specification
			if (isFirst) {
				currentOffset = entry.getByteCodeOffsetDelta();
				isFirst = false;
			} else {
				currentOffset = currentOffset + entry.getByteCodeOffsetDelta() + 1;
			}
			while (currentOffset > currentLoc.codeIndex()){
				locToNextFrame.put(currentLoc, entry);
				locToNextFrameIndex.put(currentLoc, new Integer(i));
				if (nextListIndex < locList.size()){
					currentLoc = locList.get(nextListIndex);
					nextListIndex++;
				} else {
					break StackMapTableLoop;
				}
			}
		}
	}
	
	/**
	 * Goes through the StackMapTable and calculates the byte code offset of the given entry.
	 * @param entry A StackMapTableEntry
	 * @return <code>entry</code>'s bytecode offset. -1 if it is not in this method's StackMapTable.
	 */
	private int getByteOffset(StackMapTableEntry entry){
		for (StackMapTableEntry tableEntry : _table.getStackMapTable()){
			if (tableEntry.equals(entry)){
				return totalBytecodeOffset.get(entry);
			}
		}
		return -1;
	}
	
	/**
	 * _table must already be set.
	 * @return
	 */
	private HashMap<StackMapTableEntry,Integer> constructTotalBytecodeOffsets(){
		HashMap<StackMapTableEntry,Integer> result = new HashMap<StackMapTableEntry,Integer>();
		StackMapTableEntry[] entries = _table.getStackMapTable();
		boolean isFirst = true;
		int offset = 0;
		for (StackMapTableEntry entry : entries){
			if (isFirst) {
				offset = entry.getByteCodeOffsetDelta();
				isFirst = false;
			} else {
				offset = offset + entry.getByteCodeOffsetDelta() + 1;
			}
			result.put(entry,offset);
		}
		return result;
	}
	
	/**
	 * This is for the very second 
	 * @param ih
	 * @param loc 
	 * @return
	 */
	private int getByteCodeOffsetDelta(InstructionHandle ih, Location loc){
		InstructionList il = this.getInstructionList();
		if (!il.contains(ih)){
			return -1;
		}
		il.setPositions();
		// From beginning of il
		int codeIndex = ih.getPosition();
		StackMapTableEntry[] entries = _table.getStackMapTable();
		Integer nextFrameIndex = locToNextFrameIndex.get(loc);
		Integer previousFrameIndex = (nextFrameIndex == null) ? entries.length - 1 : nextFrameIndex - 1;
		// The StackMapTableEntry that comes right before this instruction handle
		StackMapTableEntry previousFrame = (previousFrameIndex < 0) ? null : entries[previousFrameIndex];
		int delta;
		if (previousFrame == null){
			// If previousFrame has not been set, then this Handle must define the first StackMapTableEntry
			delta = codeIndex;
		} else {
			int previousFrameOffset = totalBytecodeOffset.get(previousFrame);
			// to get to codeIndex, we must add delta+1 to the previous frame's offset
			delta = codeIndex - previousFrameOffset - 1;
		}
		return delta;
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
			insertBuildCalls(loc);
//			for (TracePoint trace : locToTracePoint.get(loc)){
//				markHandle = il.findHandle((int) trace.getLoc().codeIndex());
//				il.append(markHandle, InstructionConstants.NOP);
//				StackMapTableEntry entry = locToNextFrame.get(loc);
//				if (entry != null) {
//					int delta = entry.getByteCodeOffsetDelta();
//					setOffsetDelta(entry,delta+1);
//				}
//			}
		}
	}
	
	private void insertBuildCalls(Location loc){
		InstructionList il = this.getInstructionList();
		List<StackMapTableEntry> toBeAddedFrames = new ArrayList<StackMapTableEntry>();
		Set<TracePoint> traceSet = locToTracePoint.get(loc);
		il.setPositions(true);
		InstructionHandle markHandle = il.findHandle((int) loc.codeIndex());		
		boolean isFirst = true;
		boolean isSecond = true;
		InstructionHandle veryFirstHandle = null;
		ListBranchTuple currentList;
		BranchHandle branchOfPrevious = null;
		InstructionHandle firstOfPrevious = null;
		InstructionHandle lastOfPrevious = null;
		int sizeBefore = il.getByteCode().length;
		for (TracePoint trace : traceSet){
			currentList = constructBuildCall(trace);
			InstructionHandle firstHandle = currentList.il.getStart();
			if (isFirst){
				veryFirstHandle = currentList.il.getStart();
				isFirst = false;
				// This must be set before appending because appending empties the list
				firstOfPrevious = currentList.il.getStart();
				branchOfPrevious = currentList.bh;
				lastOfPrevious = currentList.il.getEnd();
				
				il.append(markHandle, currentList.il);
			} else {
				// The first handle of the current list is now target of the branch instruction from the previous list
				branchOfPrevious.setTarget(firstHandle);
				InstructionHandle last = lastOfPrevious;
				
				firstOfPrevious = currentList.il.getStart();
				branchOfPrevious = currentList.bh;
				lastOfPrevious = currentList.il.getEnd();
				// Append, not after the markHandle, but after the previous list
				firstHandle = il.append(last, currentList.il);
				il.setPositions();
				StackMapTableEntry newEntry = this.newEntry(firstHandle, firstOfPrevious, loc, isSecond, false);
				isSecond = false;
				toBeAddedFrames.add(newEntry);
			}			
		}
		// Remove the DUP'd argument
		InstructionHandle nextHandle = il.append(lastOfPrevious, InstructionConstants.POP);
		InstructionTargeter[] targeters = markHandle.getTargeters();
		try {
			il.delete(markHandle);
		} catch (TargetLostException e) {
			// Set all targeters to markHandle to instead target the first handle of the first list that was inserted
			for (InstructionTargeter targeter : targeters){
				targeter.updateTarget(markHandle, veryFirstHandle);
			}
		}
		
		List<StackMapTableEntry> currentFrames = new ArrayList<StackMapTableEntry>();
		for (StackMapTableEntry entry : _table.getStackMapTable()){
			currentFrames.add(entry);
		}
		// Connect the last branch handle to this (can be null if previous is a NoArgTracePoints)
		if (branchOfPrevious != null){
			branchOfPrevious.setTarget(nextHandle);		
			// Create a new frame for exactly that branch
			il.setPositions(true);
			StackMapTableEntry newEntry = this.newEntry(nextHandle, firstOfPrevious, loc, isSecond, true);
			toBeAddedFrames.add(newEntry);
		} else {
			il.setPositions();
		}
		// Now, all the actual bytecode is correctly set
		int sizeAfter = il.getByteCode().length;
		// Insert all frames
		StackMapTableEntry nextFrame = locToNextFrame.get(loc);
		// If no "next frame" exists, it needs to be inserted at the end
		int startInsertionIndex = (nextFrame == null) ? currentFrames.size() : locToNextFrameIndex.get(loc);
		currentFrames.addAll(startInsertionIndex, toBeAddedFrames);
		if (nextFrame != null){
			// The bytecode offset has to be recalculated again...
			int offset = sizeAfter - sizeBefore + totalBytecodeOffset.get(nextFrame);
			int codeIndex = nextHandle.getPosition();
			// nextEntry is, by construction, not the first frame
			int delta = offset - codeIndex - 1;
			// TODO: nextFrame has one less object on the stack, the one that was constantly DUP'd and finally POP'd
			setOffsetDelta(nextFrame, delta);
		}
		StackMapTableEntry[] allFrames = new StackMapTableEntry[currentFrames.size()];
		for (int i = 0; i < currentFrames.size(); i++){
			allFrames[i] = currentFrames.get(i);
		}
		// All frames should now have the correct delta. Now, the table's size has to be set.
		int sum = 0;
		for (StackMapTableEntry frame : toBeAddedFrames){
			sum = sum + frame.getEntryByteSize();
		}
		_table.setStackMapTable(allFrames);
		_table.setLength(_table.getLength()+sum);
	}
	
	/**
	 * Call il.setPositions() first.
	 * @param firstHandle
	 * @param firstOfPrevious
	 * @param loc
	 * @param isSecond
	 * @return
	 */
	private StackMapTableEntry newEntry(InstructionHandle firstHandle, InstructionHandle firstOfPrevious, Location loc, boolean isSecond, boolean markHandleDeleted){
		int delta;						
		if (isSecond){
			// if this is the very second InstructionList that was inserted, the offset delta is from the previous of the original
			// StackMapTableEntrys to this instruction		
			delta = getByteCodeOffsetDelta(firstHandle, loc);
			// Because INVOKESTATIC is 3 bytes long
			delta = markHandleDeleted ? delta : delta - 3;
		} else {
			// delta is from the first handle of the LAST list to this first handle
			// there is already an explicit StackMapTableEntry for the last list
			delta = firstHandle.getPosition() - firstOfPrevious.getPosition() - 1;
		}
		// There are no more or less items on the stack or more or less locals. Thus, the frame type is SAME_FRAME or SAME_FRAME_EXTENDED
		// TODO: Actually, this is wrong. At every branch target, we have the DUP'd expression on top
		// Even worse, I have NO IDEA about the stack items and locals of the very first frame, the one that's defined to point at the beginning of the
		// second list.
		int frame_type = delta;
		if (delta > SAME_FRAME_MAX) {
			frame_type = SAME_FRAME_EXTENDED;
		}
		StackMapTableEntry newEntry = new StackMapTableEntry(frame_type,delta,null,null,_cp.getConstantPool());
		return newEntry;
	}
	
	/**
	 * Constructs the build call for a TracePoint. At the end of this, the argument to the mark-call is on the stack.
	 * @param trace Any TracePoint with the index field set.
	 * @return A ListBranchTuple consisting of an InstructionList that, if it's an IntTracePoint or StringTracePoint, compares the
	 * argument to the mark-call with the value saved in trace for equality and, if equal, calls the build method in the watermark
	 * class with the appropriate index.
	 * The BranchHandle in the return value is the one from the "if equal" step. Its target is not yet set, so it's very important that you
	 * do so.
	 */
	private ListBranchTuple constructBuildCall(TracePoint trace){
		int index = trace.getIndex();
		InstructionList il = new InstructionList();
		BranchInstruction ifArgEquals = null;
		BranchHandle ifArgEqualsHandle = null;
		// The result of this shall replace the mark call, so the argument to the mark-call and thus right on the stack
		if (trace instanceof NoArgTracePoint){
			il.append(_factory.createInvoke(_watermarkClassName, "buildG"+index, Type.VOID, Type.NO_ARGS, Constants.INVOKESTATIC));
			return new ListBranchTuple(il,null);
		} else {
			// Duplicate so that the argument can be used again by the next if-query.
			il.append(InstructionConstants.DUP);
			if (trace instanceof IntTracePoint){
				IntegerValue val = (IntegerValue) trace.getVal();
				int arg = val.value();
				il.append(new PUSH(_cp,arg));
				ifArgEquals = InstructionFactory.createBranchInstruction(Constants.IF_ICMPNE, null);				
			} else if (trace instanceof StringTracePoint){
				StringReference val = (StringReference) trace.getVal();
				String arg = val.value();
				il.append(new PUSH(_cp,arg));
				il.append(_factory.createInvoke("java.lang.String", "equals", Type.BOOLEAN, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
				ifArgEquals = InstructionFactory.createBranchInstruction(Constants.IFEQ, null);
			}
			ifArgEqualsHandle = il.append(ifArgEquals);
			il.append(_factory.createInvoke(_watermarkClassName, "buildG"+index, Type.VOID, Type.NO_ARGS, Constants.INVOKESTATIC));
		}			
		return new ListBranchTuple(il,ifArgEqualsHandle);
	}
	
	private void setOffsetDelta(StackMapTableEntry entry, int i){
		int type = entry.getFrameType();
		int size = entry.getEntryByteSize();
		// This is valid for CHOP, APPEND and FULL_FRAME as they always make byte offset delta explicit and as such don't
		// change types if the byte offset delta is increased
		int newType = type;
		int newDelta = i;
		
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
