package embedding;

import java.util.HashMap;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;

import tracing.TracePoint;

/**
 * A ClassGen that deals with TracePoints and inserts calls to build methods at the TracePoints' code locations.
 * @author Kaspar
 *
 */
class ClassContainer extends ClassGen{
	private static final long serialVersionUID = 1L;
	private String _watermarkClassName;
	private ConstantPoolGen _cp;
	private InstructionFactory _factory;
	private JavaClass _clazz;
	private HashMap<Method,MethodContainer> markedMethodsToContainers;
	private HashMap<String,Method> sigToMethod;

	/**
	 * Constructs a new ClassContainer.
	 * @param clazz The JavaClass that this ClassContainer should modify.
	 * @param watermarkClassName The fully qualified name of the watermark class.
	 */
	public ClassContainer(JavaClass clazz, String watermarkClassName) {
		super(clazz);
		_watermarkClassName = watermarkClassName;
		_clazz = clazz;
		_cp = getConstantPool();
		_factory = new InstructionFactory(this,_cp);
		markedMethodsToContainers = new HashMap<Method,MethodContainer>();
		Method[] methodsA = clazz.getMethods();
		sigToMethod = new HashMap<String,Method>();		
		for (Method method : methodsA){
			String sig = Tools.methodSig(clazz, method);
			sigToMethod.put(sig, method);
		}
	}
	
	/**
	 * Adds a new MethodContainer to this instance that deals with the TracePoints contained therein.
	 * @param method A Method of this class.
	 */
	private MethodContainer newMethodContainer(Method method){
		MethodContainer methodCont = markedMethodsToContainers.get(method);
		if (methodCont != null){
			return methodCont;
		}
		methodCont = new MethodContainer(method, getClassName(), _cp, _factory, _watermarkClassName);
		markedMethodsToContainers.put(method, methodCont);
		return methodCont;
	}
	
	/**
	 * Adds a TracePoint to a MethodContainer contained in this ClassContainer. There must already exist a MethodContainer in this ClassContainer whose
	 * method contains the TracePoint. 
	 * @param trace A TracePoint in a method in a MethodContainer in this instance.
	 */
	public void addTracePoint(TracePoint trace){
		String wantedSig = Tools.methodSig(trace.getLoc());
		Method method = sigToMethod.get(wantedSig);
		MethodContainer methodCont = markedMethodsToContainers.get(method);
		if(methodCont == null){
			methodCont = newMethodContainer(method);
		}
		methodCont.addTracePoint(trace);		
	}
	
	/**
	 * Processes the previously added TracePoints, adding calls to graph-building methods of the watermark class to the MethodContainers that were added. 
	 */
	public void processTracePoints(){
		for (Method method : markedMethodsToContainers.keySet()){
			MethodContainer methodCont = markedMethodsToContainers.get(method);
			methodCont.processTracePoints();
			methodCont.setMaxStack();
			methodCont.setMaxLocals();
			replaceMethod(method, methodCont.getMethod());
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((_clazz == null) ? 0 : _clazz.hashCode());
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
		ClassContainer other = (ClassContainer) obj;
		if (_clazz == null) {
			if (other._clazz != null)
				return false;
		} else if (!_clazz.equals(other._clazz))
			return false;
		return true;
	}
}
