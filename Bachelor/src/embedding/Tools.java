package embedding;

import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;

/**
 * Provides some tools for embedding and tracing.<br>
 * The <code>methodSig()</code> methods are designed in such a way that the signature for a <code>com.sun.jdi.Method</code> and a
 * <code>org.apache.bcel.classfile.Method</code> return the same signature if they stand for the same method, thus allowing if a BCEL
 * method corresponds to a JDI method or vice versa. 
 * 
 * @author Kaspar
 *
 */
public class Tools {

	private Tools() {}
	
	/**
	 * Returns the signature for a method. The format is based on what <code>javap</code> returns for the respective Methodref in the
	 * constant pool of the class.
	 * @param method The JDI method.
	 * @param type The type that the method is actually implemented in.
	 * @return The standardized signature of <code>method</code>.
	 */
	public static String methodSig(com.sun.jdi.Method method, ReferenceType type){
		String className = type.signature();
		// The declaring type is always "L" followed by the class name and ending with ";"
		className = className.substring(1, className.length()-1);
		// This signature has the same format as it is found as a "Methodref" in the constant pool in the class' Bytecode  
		String signature = className + "." + method.name() + ":" + method.signature();
		return signature;
	}
	
	/**
	 * Returns the signature for the method that <code>loc</code> belongs to. See <code>methodSig(com.sun.jdi.Method)</code> for further
	 * details.
	 * @param loc The <code>Location</code>.
	 * @return The standardized signature for the method that <code>loc</code> belongs to.
	 */
	public static String methodSig(com.sun.jdi.Location loc){
		Method method = loc.method();
		ReferenceType type = loc.declaringType();
		return methodSig(method,type);
	}
	
	/**
	 * Returns the signature for a method. The format is based on what <code>javap</code> returns for the respective Methodref in the
	 * constant pool of the class. <br>Beware:</br> This method does not check if <code>method</code> is actually a method of
	 * <code>clazz</code>, it simply returns the signature on the assumption that it is.
	 * @param method The BCEL method.
	 * @param clazz The class that <code>method</code> is declared in.
	 * @return The standardized signature of <code>method</code>.
	 */
	public static String methodSig(org.apache.bcel.classfile.JavaClass clazz, org.apache.bcel.classfile.Method method){
		String className = clazz.getClassName().replace('.', '/');
		String name = method.getName();
		String sig = method.getSignature();
		return (className+"."+name+":"+sig);
	}

}
