package embedding;

import java.util.HashMap;
import java.util.HashSet;
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
import tracing.*;

/**
 * A MethodGen that deals with TracePoints and inserts calls to build methods at the TracePoints' code locations.
 * @author Kaspar
 *
 */
class MethodContainer extends MethodGen  implements Constants{
	private static final long serialVersionUID = 1L;
	private String _watermarkClassName;
	private HashMap<Location,Set<TracePoint>> locToTracePoint;
	private InstructionFactory _factory;
	private Method method;
	private ConstantPoolGen _cp;
	
	/**
	 * Creates a new MethodContainer.
	 * @param method An existing method.
	 * @param className Fully qualified class name containing this method.
	 * @param constantPool The constant pool of the class containing this method.
	 * @param factory An instruction factory to construct new instructions.
	 * @param watermarkClassName The fully qualified name of the watermark class.
	 */
	public MethodContainer(Method method, String className, ConstantPoolGen constantPool, InstructionFactory factory, String watermarkClassName){
		super(method, className, constantPool);
		_watermarkClassName = watermarkClassName;
		this.method = method;
		this._cp = constantPool;
		locToTracePoint = new HashMap<Location,Set<TracePoint>>();
		this._factory = factory;
	}
	
	/**
	 * Adds a new trace point to this MethodContainer.
	 * @param trace A trace point with its index correctly set.
	 */
	public void addTracePoint(TracePoint trace){
		Location loc = trace.getLoc();
		Set<TracePoint> traceSet = locToTracePoint.get(loc);
		if (traceSet == null){
			traceSet = new HashSet<TracePoint>();
			locToTracePoint.put(loc, traceSet);
		}
		traceSet.add(trace);
	}
	
	/**
	 * Processes the trace points, inserting calls to graph-building methods at the trace points contained herein.
	 */
	public void processTracePoints(){
		InstructionList il = this.getInstructionList();
		il.setPositions(true);
		for (Location loc : locToTracePoint.keySet()){
			insertBuildCalls(loc);
		}
	}
	
	/**
	 * Inserts calls to graph-building methods at a specified location.
	 * @param loc A location in this method.
	 */
	private void insertBuildCalls(Location loc){
		InstructionList il = this.getInstructionList();
		Set<TracePoint> traceSet = locToTracePoint.get(loc);
		il.setPositions(true);
		InstructionHandle markHandle = il.findHandle((int) loc.codeIndex());		
		boolean isFirst = true;
		InstructionHandle veryFirstHandle = null;
		ListBranchTuple currentList;
		BranchHandle branchOfPrevious = null;
		InstructionHandle lastOfPrevious = null;
		TracePoint lastTrace = null;
		for (TracePoint trace : traceSet){
			lastTrace = trace;
			currentList = constructBuildCall(trace);
			InstructionHandle firstHandle = currentList.il.getStart();
			if (isFirst){
				veryFirstHandle = currentList.il.getStart();
				isFirst = false;
				// This must be set before appending because appending empties the list
				branchOfPrevious = currentList.bh;
				lastOfPrevious = currentList.il.getEnd();
				
				il.append(markHandle, currentList.il);
			} else {
				// The first handle of the current list is now target of the branch instruction from the previous list
				branchOfPrevious.setTarget(firstHandle);
				InstructionHandle last = lastOfPrevious;
				
				branchOfPrevious = currentList.bh;
				lastOfPrevious = currentList.il.getEnd();
				// Append, not after the markHandle, but after the previous list
				firstHandle = il.append(last, currentList.il);
				il.setPositions();
			}			
		}
		InstructionHandle nextHandle;
		// If lastTrace is a NoArgTracePoint, traceSet has exactly one element
		if (!(lastTrace instanceof NoArgTracePoint)){
			// Remove the DUP'd argument
			nextHandle = il.append(lastOfPrevious, InstructionConstants.POP);
		} else {
			nextHandle = lastOfPrevious.getNext(); 
		}
		InstructionTargeter[] targeters = markHandle.getTargeters();
		try {
			il.delete(markHandle);
		} catch (TargetLostException e) {
			// Set all targeters to markHandle to instead target the first handle of the first list that was inserted
			for (InstructionTargeter targeter : targeters){
				targeter.updateTarget(markHandle, veryFirstHandle);
			}
		}
		// Connect the last branch handle to this (can be null if previous is a NoArgTracePoints)
		if (branchOfPrevious != null){
			branchOfPrevious.setTarget(nextHandle);		
			// Create a new frame for exactly that branch
			il.setPositions(true);
		} else {
			il.setPositions();
		}
	}
		
	/**
	 * Constructs the build call for a TracePoint. At the end of this, the argument to the mark-call is on the stack if there is such an argument.
	 * @param trace Any TracePoint with the index field set.
	 * @return A ListBranchTuple consisting of an InstructionList that, if it's an IntTracePoint or StringTracePoint, compares the
	 * argument to the mark-call with the value saved in trace for equality and, if equal, calls the build method in the watermark
	 * class with the appropriate index. If trace is a NoArgTracePoint, the InstructionList just contains a call to that method.
	 * The BranchHandle in the return value is the one from the "if equal" step or null for a NoArgTracePoint. Its target is not yet set, so it's very important that you
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
