package embedding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import tracing.*;

/**
 * Instances of this class load a class file and execute the class found therein.
 * @author Kaspar
 *
 */
public class Embedder {
	
	private List<TracePoint> tracePoints;
	private HashMap<TracePoint,JavaClass> traceToClass;
	private HashMap<TracePoint,Method> traceToMethod;
	private HashMap<JavaClass,ClassContainer> classToClassCont;
	private String _watermarkClassName;

	/**
	 * Constructs a new Embedder which executes a class and identifies its TracePoints.
	 * @param classPath The path from which all necessary classes can be loaded.
	 * @param className the fully qualified name of the main class.
	 * @param args The arguments to the main class' main method.
	 * @param markMethodName the fully qualified name of the mark method(s).
	 */
	public Embedder(String classPath, String className, String[] args, String markMethodName, String watermarkClassName) {
		tracePoints = runClassFile(classPath, className, args, markMethodName);
		traceToClass = Tools.getClasses(tracePoints, classPath);
		System.out.println(Arrays.toString(tracePoints.toArray()));
		_watermarkClassName = watermarkClassName;
		traceToMethod = getMethods();
		//
		classToClassCont = new HashMap<JavaClass,ClassContainer>();
		setUpContainers();
		processTracePoints(classPath);
	}
	
	/**
	 * Initializes and builds the <code>ClassContainer</code>s for the classes that contain the <code>TracePoint</code>s in <code>tracePoints</code>.
	 * Also sets up the HashMap classToClassCont that maps each JavaClass to the ClassContainer that contains it.
	 */
	private void setUpContainers(){
		for (TracePoint trace : tracePoints){
			JavaClass clazz = traceToClass.get(trace);
			Method method = traceToMethod.get(trace);
			ClassContainer classCont;
			if (!classToClassCont.containsKey(clazz)){
				classCont = new ClassContainer(clazz, _watermarkClassName);
				classToClassCont.put(clazz, classCont);
			} else {
				classCont = classToClassCont.get(clazz);
			}
			classCont.newMethodContainer(method);
			classCont.addTracePoint(trace);
		}
	}
	
	/**
	 * Processes the trace points and saves the modified classes. 
	 */
	private void processTracePoints(String classPath){
		for (ClassContainer cont : classToClassCont.values()){
			cont.processTracePoints();
			String fullName = cont.getClassName();
			int index = fullName.lastIndexOf('.');
			String thisPackage = fullName.substring(0, index);
			String thisClass = fullName.substring(index+1,fullName.length());
			thisPackage = thisPackage.replace('.', File.separator.charAt(0));
			try {
				//TODO: Get the actual path to the .class file as assuming they are all on one path may be wrong.
				//new FileOutputStream("this.class");
				OutputStream out = new FileOutputStream(classPath+File.separator+thisPackage+File.separator+thisClass+".class");
				cont.getJavaClass().dump(out);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param classPath The path to the environment from which the main class can be called.
	 * @param className The fully qualified name of the main class.
	 * @param args The arguments to the main class' main method.
	 * @param markMethodName The fully qualified name of the mark method.
	 * @return The TracePoints that are encountered in the execution of the main class.
	 */
	private List<TracePoint> runClassFile(String classPath, String className, String[] args, String markMethodName){
		VMLauncher vmLauncher = new tracing.VMLauncher(classPath, className, args, markMethodName);
		MarkTraceThread mtt = vmLauncher.getTraceThread();
		
		// Wait until the debuggee is done
		while (mtt.isAlive()){
			
		}
		LinkedHashSet<TracePoint> tracePointsSet = null;
		try {
			tracePointsSet = mtt.getTracePoints();
		} catch (Exception e) {
			// Since we explicitly wait until the thread has finished, this should never occur.
		}
		return new ArrayList<TracePoint>(tracePointsSet);
	}
	
	/**
	 * <code>traceToClass</code> must have already been correctly initialized before this method may be called. Otherwise, bad times will occur.
	 * @param tracePoints A collection of TracePoints.
	 * @return A <code>HashMap</code> that maps every <code>TracePoint</code> found in <code>tracePoints</code> to the
	 *   <code>Method</code> that it's contained in.
	 */
	private HashMap<TracePoint,Method> getMethods(){
		HashMap<TracePoint,Method> result = new HashMap<TracePoint,Method>();
		for (TracePoint trace : traceToClass.keySet()){
			Method method = Tools.findMethod(traceToClass.get(trace), trace.getLoc());
			result.put(trace, method);
		}
		return result;
	}
	
	public static void main(String[] args){
		String classPath = args[0];
		String className = args[1];
		String markMethodName = args[args.length-1];
		String[] arguments = Arrays.copyOfRange(args, 2, args.length-1);
		new Embedder(classPath,className,arguments, markMethodName, "example.Watermark");
//		try {
//			ClassParser parser = new ClassParser("example/Nodexample.class");
//			JavaClass clazz = parser.parse();
//			BCELifier bcel = new BCELifier(clazz,System.out);
//			bcel.start();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

}
