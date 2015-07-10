package extraction;

import com.sun.jdi.VirtualMachine;

import util.VMLauncher;

public class ExtractVMLauncher extends VMLauncher {
	
	private ObjectConstructionThread oct;
	private String[] excludes = {"java.*", "javax.*", "sun.*", "com.sun.*", "org.jpgrapht.graph.*",
			"org.jgrapht.*", "java.nio.*", "oracle.*", "org.objectweb.asm.*", "javax.swing.*",
			"jdk.internal.org.*"};

	public ExtractVMLauncher(String classPath, String className, String[] args) {
		super(classPath, className, args);
		VirtualMachine vm = getVM();
		oct = new ObjectConstructionThread(vm,excludes);
		oct.start();
		vm.resume();	
	}
	
	public ObjectConstructionThread getObjectConstructionThread(){
		return oct;
	}
	
	public static void main(String[] args){
		ExtractVMLauncher extract = new ExtractVMLauncher(".", "example.Example", new String[] {"test"});
		while (extract.oct.isAlive()){}
		extract.getVM().dispose();
	}

}
