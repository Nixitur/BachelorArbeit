package extraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	public static final int ROOT_NODE = 0;
	public static final int FOOT_NODE = 5;
	/**
	 * The ObjectReference that is represented by this ObjectNode.
	 */
	public final ObjectReference or;
	private final List<ObjectReference> children;
	private static int dummyCount = 0;
	private int dummyNumber;
	private Set<Field> sameTypeFields;
	private boolean isDummyNode = false;
	
	/**
	 * Constructs a new ObjectNode. 
	 * @param or The ObjectReference that is to be interpreted as a node in an RPG.
	 */
	private ObjectNode(ObjectReference or) {
		this.or = or;
		children = new ArrayList<ObjectReference>();
		dummyNumber = -1;
		sameTypeFields = new HashSet<Field>();
	}

	/**
	 * Returns the children of this ObjectNode which are ObjectReferences of the same type as <tt>this.or</tt> that are referenced
	 * by a field value of <tt>this.or</tt>. This list is backed by this instance, so changes on it will reflect on the object. 
	 * I do not recommend changing it.
	 * @return A list of outneighbors of this ObjectNode.
	 */
	public List<ObjectReference> getChildren(){
		return children;
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
					node.sameTypeFields.add(f);
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
	 * Updates the children.
	 */
	public void updateChildren(){
		children.clear();
		for (Field f : sameTypeFields){
			Value value = or.getValue(f);
			if (value != null){
				ObjectReference valueOR = (ObjectReference) value;
				children.add(valueOR);
			}
		}
	}
	
	/**
	 * Creates a dummy node. The ObjectReference that this is a wrapper for is always null, but all dummy nodes are different and will not count as equal.
	 * @return A dummy node.
	 */
	public static ObjectNode createDummyNode(){
		ObjectNode dummy = new ObjectNode(null);
		dummy.dummyNumber = dummyCount;
		dummyCount++;
		dummy.isDummyNode = true;
		return dummy;
	}
	
	/**
	 * Constructs a String representation of this ObjectNode
	 * @return A String representation of this ObjectNode. This is the String representation of <tt>this.or</tt> followed by a new line
	 *   and the String representation of the list of children and then another new line.
	 */
	public String toString(){
		String orString = isDummyNode ? "Dummy node "+dummyNumber : ""+or.uniqueID()+" ("+or.referenceType().name()+")";
		return orString;
		//+"\n"+children.toString()+"\n"
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + dummyNumber;
		result = prime * result + ((or == null) ? 0 : or.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectNode other = (ObjectNode) obj;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (dummyNumber != other.dummyNumber)
			return false;
		if (or == null) {
			if (other.or != null)
				return false;
		} else if (!or.equals(other.or))
			return false;
		return true;
	}
}
