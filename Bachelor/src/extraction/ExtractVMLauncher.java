package extraction;

import com.sun.jdi.VirtualMachine;

import util.VMLauncher;

/**
 * Instances of this class launch a VirtualMachine for a given main class and keeps track of the last 1000 constructed objects in the 
 * target VM.
 * @author Kaspar
 *
 */
public class ExtractVMLauncher extends VMLauncher {
	
	private ObjectConstructionThread oct;
	private String[] excludes = {"java.*", "javax.*", "sun.*", "com.sun.*", "org.jpgrapht.graph.*",
			"org.jgrapht.*", "java.nio.*", "oracle.*", "org.objectweb.asm.*", "javax.swing.*",
			"jdk.internal.org.*"};

	/**
	 * Creates a new <code>ExtractVMLauncher</code> which immediately launches the Java Virtual Machine for class <code>className</code> and arguments
	 * <code>args</code>. Furthermore, an <code>ObjectConstructionThread</code> is started which tracks newly constructed objects.
	 * @param classPath The classpath by which the class may be executed.
	 * @param className The fully qualified name of the to be executed class.
	 * @param args The arguments to the class' main method.
	 */
	public ExtractVMLauncher(String classPath, String className, String[] args) {
		super(classPath, className, args);
		VirtualMachine vm = getVM();
		oct = new ObjectConstructionThread(vm,excludes);
		oct.start();
		vm.resume();	
	}
	
	/**
	 * Returns the ObjectConstructionThread which keeps track of the last 1000 constructed objects.
	 * @return the ObjectConstructionThread
	 */
	public ObjectConstructionThread getObjectConstructionThread(){
		return oct;
	}

}
