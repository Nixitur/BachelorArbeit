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
	
	VirtualMachine vm;
	private String[] excludes = {"java.*", "javax.*", "sun.*", "com.sun.*", "org.jpgrapht.graph.*",
			"org.jgrapht.*", "java.nio.*", "oracle.*", "org.objectweb.asm.*", "javax.swing.*"};

	public VMLauncher(String[] args) {
		StringBuffer classAndArgs = new StringBuffer(args[0]);
		// The main class and its command line arguments
		for (int i = 1; i < args.length; i++){
			classAndArgs.append(" "+args[i]);
		}
		String classAndArgsS = classAndArgs.toString();
		vm = launchVM(classAndArgsS);
		MarkTraceThread mtt = new MarkTraceThread(vm, excludes);
		mtt.start();
		vm.resume();
	}

	public static void main(String[] args) {
		new VMLauncher(args);
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
	 * Launches the Java Virtual Machine with a specified main class and arguments. It hereby inherits the
	 * debugger's classpath.
	 * @param classAndArgs The command line argument for the <code>java</code> program of the class that is to be executed.
	 *        The format is the class name, followed by its arguments, separated by spaces.
	 * @return
	 */
	VirtualMachine launchVM(String classAndArgs){
		LaunchingConnector cmdLineConnector = getConnector();
		Map<String,Connector.Argument> arguments = getArguments(cmdLineConnector, classAndArgs);
		try {
			VirtualMachine vm = cmdLineConnector.launch(arguments);
			Process process = vm.process();
			Thread errThread = new InToOutThread(process.getErrorStream(), System.err);
			Thread outThread = new InToOutThread(process.getInputStream(), System.out);
			errThread.start();
			outThread.start();
			return vm;
		} catch (IOException e) {
            throw new Error("Unable to launch target VM: " + e);
        } catch (IllegalConnectorArgumentsException e) {
            throw new Error("Internal error: " + e);
        } catch (VMStartException e) {
            throw new Error("Target VM failed to initialize: "+ e.getMessage());
        }
	}
}
