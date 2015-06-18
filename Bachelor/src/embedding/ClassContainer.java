package embedding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;

import tracing.TracePoint;

public class ClassContainer extends ClassGen{
	private static final long serialVersionUID = 1L;
	private ConstantPoolGen _cp;
	private InstructionFactory _factory;
	private JavaClass _clazz;
	private HashMap<Method,MethodContainer> markedMethodsToContainers;

	public ClassContainer(JavaClass clazz) {
		super(clazz);
		_clazz = clazz;
		_cp = getConstantPool();
		_factory = new InstructionFactory(this,_cp);
		markedMethodsToContainers = new HashMap<Method,MethodContainer>();
	}
	
	public void newMethodContainer(Method method){
		MethodContainer methodCont = new MethodContainer(method, getClassName(), _cp, _factory);
		markedMethodsToContainers.put(method, methodCont);
	}
	
	public void addTracePoint(TracePoint trace){
		Method method = Tools.findMethod(_clazz, trace.getLoc());
		markedMethodsToContainers.get(method).addTracePoint(trace);
	}
	
	public void processTracePoints(){
		for (Method method : markedMethodsToContainers.keySet()){
			MethodContainer methodCont = markedMethodsToContainers.get(method);
			methodCont.processTracePoints();
			methodCont.setMaxStack();
			methodCont.setMaxLocals();
			replaceMethod(method, methodCont.getMethod());
		}
	}
}
