package embedding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.classfile.JavaClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import tracing.*;
import util.TimeKeeper;

/**
 * An instance of this class loads a class file and executes the class found therein, collecting the TracePoints and finally replacing the TracePoints
 * @author Kaspar
 *
 */
public class Embedder {
	
	private List<TracePoint> tracePoints;
	private HashMap<TracePoint,JavaClass> traceToClass;
	private HashMap<JavaClass,ClassContainer> classToClassCont;
	private final String _classPath;
	private final String _args;
	private final String _markMethodName;
	private final String _className;
	private TimeKeeper time;

	/**
	 * Constructs a new Embedder which executes a class and identifies its TracePoints.
	 * @param classPath The path from which all necessary classes can be loaded.
	 * @param className the fully qualified name of the main class.
	 * @param args The arguments to the main class' main method.
	 * @param markMethodName the fully qualified name of the mark method(s).
	 */
	public Embedder(String classPath, String className, String args, String markMethodName) {
		_classPath = classPath;
		_className = className;
		_args = args;
		_markMethodName = markMethodName;
		time = new TimeKeeper("[blank]");
	}
	
	/**
	 * Runs the class provided in the constructor and extracts the TracePoints.
	 * @return The number of TracePoints that have been found.
	 */
	public int run(){
		tracePoints = runClassFile();
		traceToClass = Tools.getClasses(tracePoints, _classPath);
		System.out.println(Arrays.toString(tracePoints.toArray()));
		return tracePoints.size();
	}
	
	/**
	 * Saves the modified classes with calls to the watermark class' build methods. run() must have been called before this method.
	 * @param watermarkClassName The fully qualified name of the watermark class.
	 * @param noOfBuildMethods The number of build methods the watermark class has, indexed from 0 to (noOfBuildMethods - 1).
	 * @throws IOException If the files can not be saved.
	 */
	public void dump(String watermarkClassName, int noOfBuildMethods) throws IOException{
		classToClassCont = new HashMap<JavaClass,ClassContainer>();
		time = new TimeKeeper("wmark call replacement");
		setUpContainers(watermarkClassName, noOfBuildMethods);
		processTracePoints(_classPath);
		time.stop();
	}
	
	/**
	 * Initializes and builds the <code>ClassContainer</code>s for the classes that contain the <code>TracePoint</code>s in <code>tracePoints</code>.
	 * Also sets up the HashMap classToClassCont that maps each JavaClass to the ClassContainer that contains it.
	 * @param watermarkClassName The fully qualified name of the watermark class.
	 * @param noOfBuildMethods The number of build methods the watermark class has, indexed from 0 to (noOfBuildMethods - 1).
	 */
	private void setUpContainers(String watermarkClassName, int noOfBuildMethods){
		int indexOfCurrentTrace = 0;
		for (TracePoint trace : tracePoints){
			if (indexOfCurrentTrace >= noOfBuildMethods){
				break;
			}
			JavaClass clazz = traceToClass.get(trace);
			ClassContainer classCont;
			if (!classToClassCont.containsKey(clazz)){
				classCont = new ClassContainer(clazz, watermarkClassName);
				classToClassCont.put(clazz, classCont);
			} else {
				classCont = classToClassCont.get(clazz);
			}
			classCont.addTracePoint(trace);
			indexOfCurrentTrace++;
		}
	}
	
	/**
	 * Processes the trace points and saves the modified classes. 
	 * @param classPath The path from which all necessary classes can be loaded.
	 * @throws IOException If the file can not be saved.
	 */
	private void processTracePoints(String classPath) throws IOException{
		for (ClassContainer cont : classToClassCont.values()){
			cont.processTracePoints();
			String fullName = cont.getClassName();
			int index = fullName.lastIndexOf('.');
			String thisPackage = fullName.substring(0, index);
			String thisClass = fullName.substring(index+1,fullName.length());
			thisPackage = thisPackage.replace('.', File.separator.charAt(0));
			byte[] bytecode = cont.getJavaClass().getBytes();
			ClassReader cr = new ClassReader(bytecode);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			cr.accept(cw, 0);
			bytecode = cw.toByteArray();
			time.pause();
			//TODO: Get the actual path to the .class file as assuming they are all on one path may be wrong.				
			OutputStream out = new FileOutputStream(classPath+File.separator+thisPackage+File.separator+thisClass+".class");
			out.write(bytecode);
			out.close();
			time.resume();
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
	private List<TracePoint> runClassFile(){
		TraceVMLauncher vmLauncher = new tracing.TraceVMLauncher(_classPath, _className, _args, _markMethodName);
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
		return new LinkedList<TracePoint>(tracePointsSet);
	}
}
