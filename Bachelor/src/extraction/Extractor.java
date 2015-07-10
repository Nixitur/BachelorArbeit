package extraction;

import java.util.ArrayList;
import java.util.List;
import com.sun.jdi.ObjectReference;

public class Extractor {
	
	private final String _classPath;
	private final String[] _args;
	private final String _className;
	private ExtractVMLauncher launcher;

	public Extractor(String classPath, String className, String[] args) {
		this._className = className;
		this._classPath = classPath;
		this._args = args;
	}
	
	public void run(){
		List<ObjectReference> constructedObjects = extractConstructedObjects();
		List<ObjectNode> possibleRoots = getPossibleRoots(constructedObjects);
		constructedObjects.isEmpty();
	}
	
	private List<ObjectReference> extractConstructedObjects(){
		launcher = new ExtractVMLauncher(_classPath,_className,_args);
		ObjectConstructionThread oct = launcher.getObjectConstructionThread();
		
		while (oct.isAlive()){}
		RingBuffer<ObjectReference> constructedObjects = null;
		try {
			constructedObjects = oct.getObjects();
		} catch (Exception e) {
			// can't happen because we specifically wait for oct to die
		}
		return new ArrayList<ObjectReference>(constructedObjects);
	}
	
	private List<ObjectNode> getPossibleRoots(List<ObjectReference> constructedObjects){
		List<ObjectNode> possibleRoots = new ArrayList<ObjectNode>();
		for (ObjectReference or : constructedObjects){
			ObjectNode node = ObjectNode.checkIfValidRPGNode(or);
			if ((node != null) && (node.isValidRootNode())){
				possibleRoots.add(node);
			}
		}
		return possibleRoots;
	}
	
	public void quitVM(){
		// Let the VM run its course, finally finishing its cruelly prolonged death
		launcher.getVM().dispose();
	}

	public static void main(String[] args){
		Extractor ext = new Extractor(".", "example.Example", new String[] {"test"});
		ext.run();
		ext.quitVM();
	}
}
