package embedding;

import java.util.HashMap;

/**
 * A utility class for the watermark creator and embedder to keeps track of the local variable index of any Object.
 * It is rather limited as the index only ever increases, but since I use a very limited number of local variables, this is sufficient.
 * 
 * @author Kaspar
 *
 */
class VertexToVarIndexMap extends HashMap<Object, Integer> {
	private static final long serialVersionUID = 1L;
	private int localVarIndex;
	/**
	 * Creates a new instance, initializing the index of local variables at 0.
	 */
	public VertexToVarIndexMap(){
		super();
		localVarIndex = 0;
	}
	
	/**
	 * Inserts a new Object which has the current local variable index.
	 * @param obj The Object to be inserted.
	 */
	void put(Object obj){
		super.put(obj, localVarIndex);
		localVarIndex++;
	}
	
	/**
	 * Returns the current index of local variables.
	 * @return the local variable index
	 */
	int getLocalVarIndex(){
		return localVarIndex;
	}

}
