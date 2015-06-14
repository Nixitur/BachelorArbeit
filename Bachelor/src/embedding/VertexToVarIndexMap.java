package embedding;

import java.util.HashMap;

public class VertexToVarIndexMap extends HashMap<Integer, Integer> {
	private int localVarIndex;
	public VertexToVarIndexMap(){
		super();
		localVarIndex = 0;
	}
	
	public void put(Integer vertex){
		super.put(vertex, localVarIndex);
		localVarIndex++;
	}
	
	public int getLocalVarIndex(){
		return localVarIndex;
	}

}
