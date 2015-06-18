package embedding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Method;
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
public class MethodContainer extends MethodGen {
	private static final long serialVersionUID = 1L;
	private HashMap<Location,Set<TracePoint>> locToTracePoint;
	private InstructionFactory _factory;
	
	public MethodContainer(Method method, String className, ConstantPoolGen constantPool, InstructionFactory factory){
		super(method, className, constantPool);
		locToTracePoint = new HashMap<Location,Set<TracePoint>>();
		this._factory = factory;
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
		InstructionList il = this.getInstructionList();
		il.setPositions(true);
		InstructionHandle markHandle;
		for (Location loc : locToTracePoint.keySet()){
			for (TracePoint trace : locToTracePoint.get(loc)){
				markHandle = il.findHandle((int) trace.getLoc().codeIndex());
				il.append(markHandle, InstructionConstants.NOP);
			}
		}
	}
}
