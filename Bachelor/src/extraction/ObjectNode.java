package extraction;

import java.util.ArrayList;
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
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's a body node that has not been tampered with.
	 */
	public static final int BODY_NODE_UNBROKEN = 1;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's a body node that had its list edge removed.
	 */
	public static final int BODY_NODE_MISSING_LIST_EDGE = 2;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's a body node that had its back edge removed.
	 */
	public static final int BODY_NODE_MISSING_BACK_EDGE = 3;
	/**
	 * Possible return value for isValidBodyNode(), indicating that it's not a valid body node.
	 */
	public static final int BODY_NODE_WRONG = 4;
	public static final int FOOT_NODE = 5;
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
	 * Checks if this ObjectNode is a valid or potentially broken (by removal of one edge) body node.
	 * @param previous The ObjectReferences to the nodes that have already been found and are thus valid targets for a back edge
	 * 				   in an RPG.
	 * @return BODY_NODE_WRONG if this ObjectNode is not a valid body node and can not be fixed to be one.<br>
	 * 		   BODY_NODE_UNBROKEN if this ObjectNode is a valid body node that has not been tampered with, i.e. one edge points
	 * 		   back, one points forward.<br>
	 * 		   BODY_NODE_NO_LIST_POINTER if this ObjectNode could potentially be a body node that had its list edge removed.<br>
	 * 		   BODY_NODE_NO_BACK_POINTER if this ObjectNode could potentially be a body node that had its back edge removed.
	 */
	public int isValidBodyNode(Set<ObjectReference> previous){
		// A body node needs at least one pointer to be fixable
		if ((children.size() == 0) || (children.size() > 2)){
			return BODY_NODE_WRONG;
		}
		ObjectReference child0 = children.get(0);
		if (children.size() == 2){
			ObjectReference child1 = children.get(1);
			// One pointer needs to point back in previous, the other one needs to point forward to the unknown
			if (((previous.contains(child0)) && (!previous.contains(child1))) ||
				((previous.contains(child1)) && (!previous.contains(child0)))) {
				return BODY_NODE_UNBROKEN;
			}
			// If there's two children, but none or both of them point backwards, it is not a body node.
			return BODY_NODE_WRONG;
		}
		// We know there's only one child.
		if (previous.contains(child0)){
			// If the only child has already been found, there is no list edge
			return BODY_NODE_MISSING_LIST_EDGE;
		} else {
			// If the only child has not been found, there is no back edge
			return BODY_NODE_MISSING_BACK_EDGE;
		}
	}
	
	/**
	 * Gets the unique outneighbor via list edge from this node, if possible. 
	 * @param previous The ObjectReferences to the nodes that have already been found and are thus valid targets for a back edge
	 * 				   in an RPG.
	 * @return The unique outneighbor via list edge from this node if such exists, <tt>null</i> otherwise.
	 */
	public ObjectReference getListNeighbor(Set<ObjectReference> previous){
		int bodyNodeType = isValidBodyNode(previous);
		// If it's not a root node nor unbroken nor missing a back edge, a list edge can not be found
		if (!isValidRootNode() && (bodyNodeType != BODY_NODE_UNBROKEN) && (bodyNodeType != BODY_NODE_MISSING_BACK_EDGE)){
			return null;
		}
		ObjectReference child0 = children.get(0);
		ObjectReference child1 = (children.size() == 2) ? children.get(1) : null;

		if (previous.contains(child0)){
			ObjectReference result = (child1 == null) ? child1 : null;
			return result;
		} else if ((child1 != null) && (previous.contains(child1))){
			return child0;
		}
		return null;
	}
	
	/**
	 * Gets the unique outneighbor via back edge from this node, if possible. 
	 * @param previous The ObjectReferences to the nodes that have already been found and are thus valid targets for a back edge
	 * 				   in an RPG.
	 * @return The unique outneighbor via back edge from this node if such exists, <tt>null</i> otherwise.
	 */
	public ObjectReference getBackNeighbor(Set<ObjectReference> previous){
		int bodyNodeType = isValidBodyNode(previous);
		if ((bodyNodeType != BODY_NODE_UNBROKEN) && (bodyNodeType != BODY_NODE_MISSING_LIST_EDGE)){
			return null;
		}
		ObjectReference child0 = children.get(0);
		ObjectReference child1 = (children.size() == 2) ? children.get(1) : null;
		
		if (previous.contains(child0)){
			return child0;
		} else if ((child1 != null) && (previous.contains(child1))){
			return child1;
		}
		return null;
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
