package extraction;

import java.util.ArrayList;
import java.util.List;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

/**
 * Instances of this class describe the field structure of an ObjectReference in a way which is simple to convert to 
 * actual nodes in an RPG. They cannot be manually constructed, instead the static method <tt>checkIfValidRPGNode()</tt> is to be
 * used which checks if the provided ObjectReference can serve as a node in an RPG.
 * @author Kaspar
 *
 */
public class ObjectNode {
	/**
	 * The ObjectReference that is represented by this ObjectNode.
	 */
	public final ObjectReference or;
	private final List<ObjectReference> children;
	
	/**
	 * Constructs a new ObjectNode. 
	 * @param or The ObjectReference that is to be interpreted as a node in an RPG.
	 */
	private ObjectNode(ObjectReference or) {
		this.or = or;
		children = new ArrayList<ObjectReference>();
	}
	
	/**
	 * Checks if this ObjectNode is a valid root node, i.e. has exactly one outneighbor.
	 * @return <tt>true</tt> if it's a valid root node, false otherwise.
	 */
	public boolean isValidRootNode(){
		return (children.size() == 1);
	}
	
	/**
	 * Checks if this ObjectNode is a valid body node, i.e. has exactly two outneighbors.
	 * @return <tt>true</tt> if it's a valid body node, false otherwise.
	 */
	public boolean isValidBodyNode(){
		return (children.size() == 2);
	}
	
	/**
	 * Checks if this ObjectNode is a valid foot node, i.e. has exactly zero outneighbors.
	 * @return <tt>true</tt> if it's a valid foot node, false otherwise.
	 */
	public boolean isValidFootNode(){
		return (children.size() == 0);
	}
	
	/**
	 * Returns the children of this ObjectNode which are ObjectReferences of the same type as <tt>this.or</tt> that are referenced
	 * by a field value of <tt>this.or</tt>.
	 * @return A list of outneighbors of this ObjectNode.
	 */
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
						// The outdegree, the number of not-null outneighbors, is always the size of children
						node.children.add(valueOR);
					}
				}
			} catch (ClassNotLoadedException e) {
				return null;
			}

		}
		// My Watermark instances always have two fields of type Watermark
		// Furthermore, they may have outdegree 0, 1 or 2.
		if ((countOfSameTypeFields != 2) || (notNullValuesOfSameType > 2)){
			return null;
		}
		return node;
	}
	
	/**
	 * Constructs a String representation of this ObjectNode
	 * @return A String representation of this ObjectNode. This is the String representation of <tt>this.or</tt> followed by a new line
	 *   and the String representation of the list of children and then another new line.
	 */
	public String toString(){
		return or.toString()+"\n"+children.toString()+"\n";
	}
}
