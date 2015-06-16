package embedding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.util.BCELifier;

import com.sun.jdi.Location;

import tracing.*;

/**
 * Instances of this class load a class file and execute the class found therein.
 * @author Kaspar
 *
 */
public class Embedder {
	
	private JavaClass clazz = null;
	private List<TracePoint> tracePoints;
	private HashMap<String,JavaClass> fileToClass;
	private HashMap<Location,String> locToFile;
	private HashMap<Location,Method> locToBCELMethod;
	private HashMap<Location,List<TracePoint>> locToTracePoint;

	/**
	 * Constructs a new Embedder which executes a class and identifies its TracePoints.
	 * @param args The first element is the path to the class file. The following elements are arguments
	 * 			   to the <code>main()</code> method of the class in the class file.
	 */
	public Embedder(String pathToClassFile, String[] args, String markMethodName) {
		tracePoints = runClassFile(pathToClassFile, args, markMethodName);
		System.out.println(Arrays.toString(tracePoints.toArray()));
		locToFile = Tools.getFileNames(tracePoints);
		fileToClass = Tools.getClasses(locToFile.values());
		locToBCELMethod = getMethods();
		locToTracePoint = Tools.getLocToTracePoints(tracePoints);
	}
	
	/**
	 * 
	 * @param args The first element is the path to the class file. The following elements are arguments
	 * 			   to the <code>main()</code> method of the class in the class file.
	 * @param markMethodName The full name of the mark method. This needs to contain the package as well as the class, so
	 *  for example "mark" would not be correct, but "myPackage.Marker.mark" would be.
	 * @return
	 */
	private List<TracePoint> runClassFile(String pathToClassFile, String[] args, String markMethodName){
		try {
			ClassParser parser = new ClassParser(pathToClassFile);
			clazz = parser.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String name = clazz.getClassName();
		VMLauncher vmLauncher = new tracing.VMLauncher(name, args, markMethodName);
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
	 * <code>locToFile</code> and <code>fileToClass</code> must have already been correctly initialized before this
	 * method may be called. Otherwise, bad times will occur.
	 * @return A <code>HashMap</code> that maps every <code>Location</code> found in <code>locToFile</code> to the
	 *   <code>Method</code> that it's contained in.
	 */
	private HashMap<Location,Method> getMethods(){
		HashMap<Location,Method> result = new HashMap<Location,Method>();
		String file;
		for (Location loc : locToFile.keySet()){
			file = locToFile.get(loc);
			Method method = Tools.findMethod(fileToClass.get(file), loc);
			result.put(loc, method);
		}
		return result;
	}
	
	public static void main(String[] args){
		String pathToClassFile = args[0];
		String markMethodName = args[args.length-1];
		String[] arguments = Arrays.copyOfRange(args, 1, args.length-1);
		new Embedder(pathToClassFile,arguments, markMethodName);
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
