package embedding;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import com.sun.jdi.Location;

import tracing.*;

/**
 * Instances of this class load a class file and execute the class found therein.
 * @author Kaspar
 *
 */
public class ClassExecutor {
	
	private JavaClass clazz = null;
	private List<TracePoint> tracePoints;
	private HashMap<String,JavaClass> fileToClass;
	private HashMap<Location,String> locToFile;
	private HashMap<Location,Method> locToBCELMethod;

	/**
	 * Constructs a new ClassExecutor 
	 * @param args The first element is the path to the class file. The following elements are arguments
	 * 			   to the <code>main()</code> method of the class in the class file.
	 */
	public ClassExecutor(String[] args) {
		tracePoints = runClassFile(args);
		System.out.println(Arrays.toString(tracePoints.toArray()));
		locToFile = getFileNames();
		fileToClass = getClasses();
		locToBCELMethod = getMethods();
		for (Location loc : locToBCELMethod.keySet()){
			System.out.println(Tools.methodSig(loc)+"\n   :"+locToBCELMethod.get(loc).toString());
		}
	}
	
	private List<TracePoint> runClassFile(String[] args){
		String sourceName = args[0];
		try {
			ClassParser parser = new ClassParser(sourceName);
			clazz = parser.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String name = clazz.getClassName();
		args[0] = name;
		VMLauncher vmLauncher = new tracing.VMLauncher(args);
		MarkTraceThread mtt = vmLauncher.getTraceThread();
		
		// Wait until the debuggee is done
		while (vmLauncher.getTraceThread().isAlive()){
			
		}
		return mtt.getTracePoints();
	}
	
	private org.apache.bcel.classfile.Method findMethod(JavaClass clazz, com.sun.jdi.Location loc){
		String wantedSig = Tools.methodSig(loc);
		Method[] methods = clazz.getMethods();
		Method testMethod;
		for (int i = 0; i < methods.length; i++){
			testMethod = methods[i];
			if (Tools.methodSig(clazz, testMethod).equals(wantedSig)){
				return testMethod;
			}
		}
		return null;
	}
	
	private HashMap<Location,String> getFileNames(){
		HashMap<Location,String> result = new HashMap<Location,String>();
		Location loc;
		String sig;
		int i;
		for (TracePoint tracePoint : tracePoints){
			loc = tracePoint.getLoc();
			sig = Tools.methodSig(loc);
			i = sig.indexOf('.');
			sig = sig.substring(0, i+1)+"class";
			result.put(loc, sig);
		}
		return result;
	}
	
	private HashMap<String,JavaClass> getClasses(){
		HashMap<String,JavaClass> result = new HashMap<String,JavaClass>();
		ClassParser parser;
		JavaClass thisClass = null;
		for (String sig : locToFile.values()){			
			if (!result.containsKey(sig)){
				try {
					parser = new ClassParser(sig);
					thisClass = parser.parse();
				} catch (IOException e) {
					e.printStackTrace();
				}
				result.put(sig, thisClass);
			}
		}
		return result;
	}
	
	private HashMap<Location,Method> getMethods(){
		HashMap<Location,Method> result = new HashMap<Location,Method>();
		String file;
		for (Location loc : locToFile.keySet()){
			file = locToFile.get(loc);
			Method method = this.findMethod(fileToClass.get(file), loc);
			result.put(loc, method);
		}
		return result;
	}
	
	public static void main(String[] args){
		new ClassExecutor(args);		
	}

}
