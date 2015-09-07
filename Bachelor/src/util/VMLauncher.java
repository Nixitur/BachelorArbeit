package util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

/**
 * Instances of this class simply launch a VirtualMachine for a given class. This class is designed to be extended by implementations
 * that actually do something.
 * @author Kaspar
 *
 */
public class VMLauncher {
	private VirtualMachine vm;

	/**
	 * Creates a new <code>VMLauncher</code> which immediately launches the Java Virtual Machine for class <code>className</code> and arguments
	 * <code>args</code>. The VM contained herein is automatically suspended.
	 * @param classPath The classpath by which the class may be executed.
	 * @param className The fully qualified name of the to be executed class.
	 * @param args The arguments to the class' main method.
	 */
	public VMLauncher(String classPath, String className, String args) {
		this.vm = launchVMAndSuspend(classPath, className,args);
		Process process = vm.process();
		Thread errThread = new InToOutThread(process.getErrorStream(), System.err);
		Thread outThread = new InToOutThread(process.getInputStream(), System.out);
		errThread.start();
		outThread.start();
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
	private Map<String,Connector.Argument> getArguments(String classPath, LaunchingConnector connector, String classAndArgs){
		Map<String,Connector.Argument> arguments = connector.defaultArguments();
		// It needs the proper main class and arguments
		Connector.Argument mainArg = arguments.get("main");
		mainArg.setValue(classAndArgs);
		
		// Make target VM inherit the classpath
		Connector.Argument optionArg = arguments.get("options");
		String thisClasspath = System.getProperty("java.class.path");
        optionArg.setValue("-cp "+classPath+";"+thisClasspath);
                
        return arguments;
	}
	
	/**
	 * Launches the Java Virtual Machine with a specified main class and arguments and immediately suspends it. It hereby inherits the
	 * debugger's classpath. Remember to resume the VM.
	 * @param classPath The classpath.
	 * @param className The full class name for the to be executed class.
	 * @param args The arguments to the class' main method.
	 * @return
	 */
	public VirtualMachine launchVMAndSuspend(String classPath, String className, String args){
		String classAndArgs = className+" "+args;
		
		LaunchingConnector cmdLineConnector = getConnector();
		Map<String,Connector.Argument> arguments = getArguments(classPath, cmdLineConnector, classAndArgs);
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
	
	public VirtualMachine getVM(){
		return vm;
	}
}
