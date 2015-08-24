package embedding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import tracing.TracePoint;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;

/**
 * Provides some tools for embedding and tracing.<br>
 * The <code>methodSig()</code> methods are designed in such a way that the signature for a <code>com.sun.jdi.Method</code> and a
 * <code>org.apache.bcel.classfile.Method</code> return the same signature if they stand for the same method, thus allowing to check if a BCEL
 * method corresponds to a JDI method and vice versa. 
 * 
 * @author Kaspar
 *
 */
public class Tools {

	/**
	 * No creation of instances necessary or desirable.
	 */
	private Tools() {}
	
	static <T> void fillWithNull(List<T> list, int n){
		for (int i = 0; i < n; i++){
			list.add(null);
		}
	}
	
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
	 * Returns the signature for the method that <code>loc</code> belongs to. See <code>methodSig(com.sun.jdi.Method, ReferenceType)</code> for further
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
	 * constant pool of the class. <b>Beware:</b> This method does not check if <code>method</code> is actually a method of
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
	
	/**
	 * Splits nodes of a graph with a specified Hamilton Path into several lists. These sublists are as close to
	 * each other in terms of size as possible.
	 * @param hamiltonPath The Hamilton Path through a graph where all nodes are Integers.
	 * @param n The number of lists the path is to be split up in. If this is larger than the size of the Hamilton Path, it will be set to 
	 * (hamiltonPath.size() - 1).
	 * @return A list of lists of nodes. Each list of nodes is in ascending order of index in <code>hamiltonPath</code>
	 *   while the lists themselves are in descending order of index of the first element in the list.<br>
	 *   <br>
	 *   For example, if the list<br>
	 *   [0, 1, 2, 3, 4, 5, 6, 7, 8]<br>
	 *   is split up into three lists, the return value is<br>
	 *   [[6, 7, 8], [3, 4, 5], [0, 1, 2]]
	 */
	 static List<List<Integer>> splitNodes(List<Integer> hamiltonPath, int n){
		// Copy hamiltonPath so as to not overwrite it
		List<Integer> path = new ArrayList<Integer>(hamiltonPath);
		int length = path.size();
		if (n > length){
			n = length - 1;
		}
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		// length of one slice
		int m = length / n;
		// how many need to be overfilled
		int p = length % n;
		int pTracker = 1;
		List<Integer> list;
		int index = length-1;
		for (int i = 0; i < n; i++){
			if (i >= p){
				pTracker = 0;
			}
			list = new ArrayList<Integer>();
			for (int j = 0; j < m+pTracker; j++){
				list.add(path.get(index));
				index--;
			}
			result.add(list);
		}
		list = result.get(n-1);
		for (int i = index; i >= 0; i--){
			list.add(path.get(i));
		}
		for (int i = 0; i < n; i++){
			list = result.get(i);
			Collections.reverse(list);
		}
		return result;
	}
	
	/**
	 * Generates a <code>String</code> representation of a list of lists. 
	 * @param list The list of lists that is to be turned into a <code>String</code>.
	 * @return A <code>String</code> representation of <code>list</code>.
	 */
	public static <S> String listListToString(List<List<S>> list){
		StringBuffer buf = new StringBuffer();
		List<S> sublist;
		buf.append('[');		
		for (int i = 0; i < list.size(); i++){
			sublist = list.get(i);
			buf.append('[');
			for (int j = 0; j < sublist.size(); j++){
				buf.append(sublist.get(j).toString());
				buf.append(", ");
			}
			buf.setLength(buf.length()-2);
			buf.append(']');
			buf.append(", ");
		}
		buf.setLength(buf.length()-2);
		buf.append(']');
		return buf.toString();
	}

	/**
	 * Finds a BCEL method that contains a given code location in a given class.
	 * @param clazz The class that the code location is found in.
	 * @param loc The code location.
	 * @return The BCEL method that is a method in <code>clazz</code> and contains the Location <code>loc</code>
	 *   in it. <code>null</code> if no such method is found.
	 */
	static org.apache.bcel.classfile.Method findMethod(JavaClass clazz, com.sun.jdi.Location loc){
		String wantedSig = methodSig(loc);
		org.apache.bcel.classfile.Method[] methods = clazz.getMethods();
		org.apache.bcel.classfile.Method testMethod;
		for (int i = 0; i < methods.length; i++){
			testMethod = methods[i];
			if (methodSig(clazz, testMethod).equals(wantedSig)){
				return testMethod;
			}
		}
		return null;
	}

	/**
	 * Parses .class files and returns a Map that maps the paths to these files to the corresponding <code>JavaClass</code>.
	 * @param pathsToClassFiles Contains paths to .class files relative to the working directory.
	 * @return Maps each path of a .class file to its parsed <code>JavaClass</code>.
	 */
	static HashMap<TracePoint,JavaClass> getClasses(Collection<TracePoint> tracePoints, String classPath){
		HashMap<TracePoint,JavaClass> result = new HashMap<TracePoint,JavaClass>();
		ClassParser parser;
		JavaClass clazz = null;
		String sig;
		for (TracePoint trace : tracePoints){
			sig = methodSig(trace.getLoc());
			int i = sig.indexOf('.');
			sig = sig.substring(0, i+1)+"class";
			if (!result.containsKey(trace)){
				try {
					// TODO: This mostly works, I guess, but there may be multiple class paths. Maybe use a URLClassLoader?
					parser = new ClassParser(classPath+"/"+sig);
					clazz = parser.parse();
				} catch (IOException e) {
					e.printStackTrace();
				}
				result.put(trace, clazz);
			}
		}
		return result;
	}

	/**
	 * Maps the locations found in a <code>List</code> of <code>TracePoint</code>s to a <code>List</code> of <code>TracePoint</code>s
	 * at that <code>Location</code>.
	 * @param tracePoints Any old <code>List</code> of <code>TracePoint</code>s.
	 * @return A <code>HashMap</code> that maps each <code>Location</code> found in one of the <code>TracePoint</code>s
	 *   in <code>tracePoints</code> to a <code>List</code> of <code>TracePoint</code>s at that <code>Location</code>.
	 *   The order of appearance of each <code>Location</code> in <code>tracePoints</code> is preserved in the sublists.
	 */
	static HashMap<Location,List<TracePoint>> getLocToTracePoints(List<TracePoint> tracePoints){
		HashMap<Location,List<TracePoint>> result = new HashMap<Location,List<TracePoint>>();
		Location loc;
		List<TracePoint> list;
		for (TracePoint tracePoint : tracePoints){
			loc = tracePoint.getLoc();
			list = result.get(loc);
			if (list == null){
				list = new ArrayList<TracePoint>();
				result.put(loc, list);
			}
			list.add(tracePoint);
		}
		return result;
	}
}
