package tracing;

import com.sun.jdi.VirtualMachine;

public class TraceVMLauncher extends util.VMLauncher{
	

	private MarkTraceThread mtt;
	private String[] excludes = {"java.*", "javax.*", "sun.*", "com.sun.*", "org.jpgrapht.graph.*",
			"org.jgrapht.*", "java.nio.*", "oracle.*", "org.objectweb.asm.*", "javax.swing.*",
			"jdk.internal.org.*"};

	/**
	 * Creates a new <code>VMLauncher</code> which immediately launches the Java Virtual Machine for class <code>className</code> and arguments
	 * <code>args</code>. Furthermore, a <code>MarkTraceThread</code> is started which tracks calls to a mark method.
	 * @param classPath The classpath by which the class may be executed.
	 * @param className The fully qualified name of the to be executed class.
	 * @param args The arguments to the class' main method.
	 * @param markMethodName The full name of the mark method. This needs to contain the package as well as the class, so
	 *  for example, "mark" would not be correct, but "myPackage.Marker.mark" would be if myPackage is a package containing a class Marker
	 *  containing a method mark.
	 */
	public TraceVMLauncher(String classPath, String className, String[] args, String markMethodName) {
		super(classPath, className, args);
		VirtualMachine vm = getVM();
		mtt = new MarkTraceThread(vm, excludes, markMethodName);
		mtt.start();
		vm.resume();
	}
	
	/**
	 * Returns the <code>MarkTraceThread</code> that waits for calls to a mark-function.
	 * @return the <code>MarkTraceThread</code>
	 */
	public MarkTraceThread getTraceThread(){
		return mtt;
	}
}
