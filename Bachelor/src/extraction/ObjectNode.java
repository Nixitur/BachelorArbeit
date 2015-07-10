package extraction;

import java.util.ArrayList;
import java.util.List;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

public class ObjectNode {
	public final ObjectReference or;
	private final List<ObjectReference> children;
	private ObjectNode(ObjectReference or) {
		this.or = or;
		children = new ArrayList<ObjectReference>();
	}
	
	public boolean isValidRootNode(){
		return (children.size() == 1);
	}
	
	public List<ObjectReference> getChildren(){
		return new ArrayList<ObjectReference>(children);
	}
	
	/**
	 * Checks if the provided ObjectReference could be the node of a reducible permutation graph.
	 * @param or The ObjectReference referencing an object in the target VM.
	 * @return A new ObjectNode if the argument could be the node of an RPG; <tt>null</tt> otherwise. 
	 */
	public static ObjectNode checkIfValidRPGNode(ObjectReference or){
		ObjectNode node = new ObjectNode(or);
		ReferenceType type = or.referenceType();
		List<Field> fields = type.allFields();
		int countOfSameTypeFields = 0;
		int notNullValuesOfSameType = 0;
		for (Field f : fields){
			// The outneighbor's type must be the same as this node's
			try {
				if (f.type().equals(type)){						
					countOfSameTypeFields++;
					// only up to 2 edges are allowed
					if (countOfSameTypeFields > 2){							
						break;
					}
					Value value = or.getValue(f);
					// null values are mirrored as null values which is handy
					if (value != null){
						notNullValuesOfSameType++;
						ObjectReference valueOR = (ObjectReference) value;
						node.children.add(valueOR);
					}
				}
			} catch (ClassNotLoadedException e) {
				return null;
			}

		}
		// In RPGs, nodes have up to two outneighbors
		if ((countOfSameTypeFields != 2) || (notNullValuesOfSameType < 1) || (notNullValuesOfSameType > 2)){
			return null;
		}
		return node;
	}
	
	public String toString(){
		return or.toString()+"\n"+children.toString()+"\n";
	}
}
