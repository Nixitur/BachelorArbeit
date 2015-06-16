package tracing;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

public class VMLauncher {
	
	private MarkTraceThread mtt;
	private String[] excludes = {"java.*", "javax.*", "sun.*", "com.sun.*", "org.jpgrapht.graph.*",
			"org.jgrapht.*", "java.nio.*", "oracle.*", "org.objectweb.asm.*", "javax.swing.*",
			"jdk.internal.org.*"};

	/**
	 * Creates a new <code>VMLauncher</code> which immediately launches the Java Virtual Machine for class <code>className</code> and arguments
	 * <code>args</code>. Furthermore, a <code>MarkTraceThread</code> is started which tracks calls to a mark method.
	 * @param className The full class name of the to be executed class.
	 * @param args The arguments to the class' main method.
	 * @param markMethodName The full name of the mark method. This needs to contain the package as well as the class, so
	 *  for example, "mark" would not be correct, but "myPackage.Marker.mark" would be if myPackage is a package containing a class Marker
	 *  containing a method mark.
	 */
	public VMLauncher(String className, String[] args, String markMethodName) {
		VirtualMachine vm = launchVMAndSuspend(className,args);
		Process process = vm.process();
		Thread errThread = new InToOutThread(process.getErrorStream(), System.err);
		Thread outThread = new InToOutThread(process.getInputStream(), System.out);
		mtt = new MarkTraceThread(vm, excludes, markMethodName);
		errThread.start();
		outThread.start();		
		mtt.start();
		vm.resume();
	}
	
	/**
	 * Returns the Sun Command Line Launching Connector.
	 * @return The Sun Command Line Launching Connector.
	 */
	private LaunchingConnector getConnector(){
		List<LaunchingConnector> connectors = Bootstrap.virtualMachineManager().launchingConnectors();
		for (LaunchingConnector connector : connectors){
			if (connector.name().equals("com.sun.jdi.CommandLineLaunch")){
				return connector;
			}
		}
		throw new Error("No launching connector");
	}
	
	/**
	 * Returns the arguments to be used in the Sun Command Line Launching Connector.
	 * @param connector The Sun Command Line Launching Connector.
	 * @param classAndArgs The command line argument for the <code>java</code> program of the class that is to be executed.
	 *        The format is the class name, followed by its arguments, separated by spaces.
	 * @return
	 */
	private Map<String,Connector.Argument> getArguments(LaunchingConnector connector, String classAndArgs){
		Map<String,Connector.Argument> arguments = connector.defaultArguments();
		// It needs the proper main class and arguments
		Connector.Argument mainArg = arguments.get("main");
		mainArg.setValue(classAndArgs);
		
		// Make target VM inherit the classpath
		Connector.Argument optionArg = arguments.get("options");
		String classpath = System.getProperty("java.class.path");
        optionArg.setValue("-cp "+classpath);
        
        return arguments;
	}
	
	/**
	 * Launches the Java Virtual Machine with a specified main class and arguments and immediately suspends it. It hereby inherits the
	 * debugger's classpath. Remember to resume the VM.
	 * @param className The full class name for the to be executed class.
	 * @param args The arguments to the class' main method.
	 * @return
	 */
	VirtualMachine launchVMAndSuspend(String className, String[] args){		
		StringBuffer classAndArgsBuf = new StringBuffer(className);
		// The main class and its command line arguments
		for (int i = 0; i < args.length; i++){
			classAndArgsBuf.append(" "+args[i]);
		}
		String classAndArgs = classAndArgsBuf.toString();
		
		LaunchingConnector cmdLineConnector = getConnector();
		Map<String,Connector.Argument> arguments = getArguments(cmdLineConnector, classAndArgs);
		try {
			VirtualMachine vm = cmdLineConnector.launch(arguments);
			vm.suspend();
			return vm;
		} catch (IOException e) {
            throw new Error("Unable to launch target VM: " + e);
        } catch (IllegalConnectorArgumentsException e) {
            throw new Error("Internal error: " + e);
        } catch (VMStartException e) {
            throw new Error("Target VM failed to initialize: "+ e.getMessage());
        }
	}
	
	/**
	 * Returns the <code>MarkTraceThread</code> that waits for calls to a mark-function.
	 * @return the <code>MarkTraceThread</code>
	 */
	public MarkTraceThread getTraceThread(){
		return mtt;
	}
}
