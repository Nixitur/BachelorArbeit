package embedding;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import tracing.*;

/**
 * Instances of this class load a class file and execute the class found therein.
 * @author Kaspar
 *
 */
public class ClassExecutor {
	
	private JavaClass thisClass = null;
	private List<TracePoint> tracePoints;

	/**
	 * Constructs a new ClassExecutor 
	 * @param args The first element is the path to the class file. The following elements are arguments
	 * 			   to the <code>main()</code> method of the class in the class file.
	 */
	public ClassExecutor(String[] args) {
		String sourceName = args[0];
		try {
			ClassParser parser = new ClassParser(sourceName);
			thisClass = parser.parse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String name = thisClass.getClassName();
		args[0] = name;
		VMLauncher vmLauncher = new tracing.VMLauncher(args);
		MarkTraceThread mtt = vmLauncher.getTraceThread();
		
		// Wait until the debuggee is done
		while (vmLauncher.getTraceThread().isAlive()){
			
		}
		tracePoints = mtt.getTracePoints();
		System.out.println(Arrays.toString(tracePoints.toArray()));
	}
	
	public static void main(String[] args){
		ClassExecutor exec = new ClassExecutor(args);
		JavaClass thisClass = exec.thisClass;
		List<TracePoint> tracePoints = exec.tracePoints;
	}

}
