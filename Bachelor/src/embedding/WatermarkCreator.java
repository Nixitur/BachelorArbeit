package embedding;

import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class WatermarkCreator implements Constants {
	private final String		ARRAY_NAME = "array";
	private final String		E1_NAME = "e1";
	private final String		E2_NAME = "e2";
	private final ObjectType 	NODE_TYPE;
	private final String		CLASS_NAME;	
	private InstructionFactory 	_factory;
	private ConstantPoolGen 	_cp;
	private ClassGen 			_cg;
	private DirectedGraph<Integer,DefaultEdge> _graph;
	private int					_noOfVertices;
	private List<List<Integer>> _splitNodes;
	private HashMap<Integer,Integer> remainingNeighbors;
	
	public WatermarkCreator(String packageName, DirectedGraph<Integer,DefaultEdge> graph, int noOfSubgraphs) {
		CLASS_NAME = packageName+".Watermark";
		_cg = new ClassGen(CLASS_NAME, "java.lang.Object", "Watermark.java", ACC_PUBLIC | ACC_SUPER, new String[] {  });
		_cp = _cg.getConstantPool();
		_factory = new InstructionFactory(_cg,_cp);
		_graph = graph;
		int n = graph.vertexSet().size();
		List<Integer> hamiltonPath = encoding.Tools.hamiltonPath(n);
		_splitNodes = Tools.splitNodes(hamiltonPath, noOfSubgraphs);
		NODE_TYPE = new ObjectType(CLASS_NAME);
		_noOfVertices = n;
		remainingNeighbors = noOfNeighbors();
	}
	
	/**
	 * Counts the number of neighbors of each vertex of <code>_graph</code>.
	 * @return A <code>HashMap</code> that associates each vertex (key) with the number of neighbors that vertex has (value).
	 */
	private HashMap<Integer,Integer> noOfNeighbors(){
		HashMap<Integer,Integer> result = new HashMap<Integer,Integer>();
		for (Integer vertex : _graph.vertexSet()){
			result.put(vertex, _graph.inDegreeOf(vertex)+_graph.outDegreeOf(vertex));
		}
		return result;
	}
	
	/**
	 * Reduces the count of remaining neighbors by one for both vertices. They must already be as keys in <code>remainingNeighbors</code>.
	 * @param v1 The one vertex.
	 * @param v2 The other vertex.
	 */
	private void reduceRemainingNeighbors(Integer v1, Integer v2){
		remainingNeighbors.put(v1, remainingNeighbors.get(v1)-1);
		remainingNeighbors.put(v2, remainingNeighbors.get(v2)-1);
	}
	
	/**
	 * Returns an instruction that accesses <code>array</code> and pushes it on the stack.
	 * @return The very same instruction.
	 */
	private FieldInstruction getArray(){
		return _factory.createFieldAccess(CLASS_NAME, ARRAY_NAME, new ArrayType(NODE_TYPE, 1), GETSTATIC);
	}
	
	/**
	 * Creates code that loads two Watermark instances and connects them via field assignment. This is equivalent to creating an edge.
	 * For example, if
	 * the instance that stands for <code>sourceVertex</code> is <code>o1</code>  and the instance that stands for
	 * <code>targetVertex</code> is </code>o2</code>, the assignment is<br>
	 * <code>o1.fieldName = o2</code><br> 
	 * @param sourceVertex The source vertex of the edge.
	 * @param targetVertex The target vertex of the edge.
	 * @param fieldName The field name on the instance that stands for <code>sourceVertex</code> that is to be set to the instance that stands
	 *   for <code>targetVertex</code>
	 * @param vertexToVarIndex Assigns to each vertex a local variable index.
	 * @param il The instruction list that is appended to by this method.
	 */
	private void createEdge(Integer sourceVertex, Integer targetVertex, String fieldName, VertexToVarIndexMap vertexToVarIndex, 
			InstructionList il){
		int sourceIndex = vertexToVarIndex.get(sourceVertex).intValue();
		int targetIndex = vertexToVarIndex.get(targetVertex).intValue();
		il.append(_factory.createLoad(Type.OBJECT, sourceIndex));
		il.append(_factory.createLoad(Type.OBJECT, targetIndex));
		il.append(_factory.createFieldAccess(CLASS_NAME, E1_NAME, NODE_TYPE, PUTFIELD));
		// reduce the count of how many neighbors are remaining for both nodes
		reduceRemainingNeighbors(sourceVertex,targetVertex);
	}
	
	/**
	 * Creates code that creates Watermark instances that stand for the nodes in <code>nodes</code>.
	 * @param nodes The nodes that are to be turned into code.
	 * @param il The instruction list that is appended to by this method.
	 * @param vertexToVarIndex Assigns to each vertex a local variable index.
	 */
	private void createGiNodes(List<Integer> nodes, InstructionList il, VertexToVarIndexMap vertexToVarIndex){
		for (int j = 0; j < nodes.size(); j++){
			// create a brand new node and push it on stack
			InstructionHandle createNewNode = il.append(_factory.createNew(CLASS_NAME));
			// duplicate the object on the stack
			il.append(InstructionConstants.DUP);
			// pop one from the stack and invoke constructor on it 
			il.append(_factory.createInvoke(CLASS_NAME, "<init>", Type.VOID, Type.NO_ARGS, INVOKESPECIAL));
			// pop the other one and store it in local variable j+1 (or just j if this is G_0)
			il.append(_factory.createStore(Type.OBJECT, vertexToVarIndex.getLocalVarIndex()));
			vertexToVarIndex.put(nodes.get(j));
		}
	}
	
	/**
	 * Creates code that creates
	 * @param treeNeighbors
	 * @param il
	 * @param vertexToVarIndex
	 */
	private void createTreeNeighbors(TreeNeighborMap treeNeighbors, InstructionList il, VertexToVarIndexMap vertexToVarIndex){
		Integer outNeighbor;
		for (Integer vertex : treeNeighbors.keySet()){
			outNeighbor = treeNeighbors.get(vertex);
			// if this vertex hasn't been stored yet
			if (vertexToVarIndex.get(outNeighbor) == null){
				// push array on stack
				il.append(getArray());
				// push desired index on stack
				il.append(new PUSH(_cp, outNeighbor.intValue()));
				// load array[index] and push it on stack
				il.append(InstructionConstants.AALOAD);
				// and store it in local variable
				il.append(_factory.createStore(Type.OBJECT, vertexToVarIndex.getLocalVarIndex()));
				vertexToVarIndex.put(outNeighbor);
			}
		}
	}
	
	private void createCheckIfArrayNull(InstructionList il){
		// push array on stack
		il.append(getArray());
		// check if top of stack is not null; if so, go to an as of yet undetermined (null) instruction
		BranchInstruction ifArrayNonNull = _factory.createBranchInstruction(IFNONNULL, null);
		il.append(ifArrayNonNull);
		// push the number of vertices on the stack
		InstructionHandle pushN = il.append(new PUSH(_cp, _noOfVertices));
		// push new array (with 1 dimension and length on top of stack) on stack
		il.append(_factory.createNewArray(NODE_TYPE, (short) 1));
		// take what is on top of the stack and save it as the field ARRAY_NAME
		il.append(_factory.createFieldAccess(CLASS_NAME, ARRAY_NAME, new ArrayType(NODE_TYPE,1), PUTSTATIC));
		
		// Just wait for a bit
		InstructionHandle nop = il.append(InstructionConstants.NOP);
		// jump to this instruction if array was not null
		ifArrayNonNull.setTarget(nop);
	}

	private void createFields(){
		FieldGen field;
		
		field = new FieldGen(ACC_PUBLIC, NODE_TYPE, E1_NAME, _cp);
		_cg.addField(field.getField());
		
		field = new FieldGen(ACC_PUBLIC, NODE_TYPE, E2_NAME, _cp);
		_cg.addField(field.getField());
		
		field = new FieldGen(ACC_PUBLIC | ACC_STATIC, new ArrayType(NODE_TYPE, 1), ARRAY_NAME, _cp);
		_cg.addField(field.getField());
	}
	
	private void createConstructor(){
		InstructionList il = new InstructionList();
		// Constructor is public, has no return type, no arguments, is called <init> and is in Watermark
		MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {}, "<init>", CLASS_NAME, il, _cp);
		
		// Push the name of Object class on stack
		InstructionHandle loadObjectClass = il.append(_factory.createLoad(Type.OBJECT, 0));
		// Invoke <init> on class that is on the stack
		il.append(_factory.createInvoke("java.lang.Object", "<init>", Type.VOID, Type.NO_ARGS, INVOKESPECIAL));
		InstructionHandle returnInstruction = il.append(_factory.createReturn(Type.VOID));
		method.setMaxStack();
		method.setMaxLocals();
		_cg.addMethod(method.getMethod());
		il.dispose();
	}
	
	private void createBuildGi(int i){
		List<Integer> nodes = _splitNodes.get(i);
		Integer lastOfPrevious = null;
		if (i > 0){
			lastOfPrevious = nodes.get(0)+1;
		}
		
		InstructionList il = new InstructionList();
		MethodGen method = new MethodGen(ACC_PUBLIC | ACC_STATIC, Type.VOID, Type.NO_ARGS, new String[] {}, "buildG"+i, CLASS_NAME, il, _cp);
		
		createCheckIfArrayNull(il);
		
		// Keep track of the indices that all our local variables have
		VertexToVarIndexMap vertexToVarIndex = new VertexToVarIndexMap();

		// If this is not G_0
		if (lastOfPrevious != null){
			// push array on stack
			il.append(getArray());
			// put desired index on stack
			il.append(new PUSH(_cp, lastOfPrevious.intValue()));
			// load array[index] and push it on stack
			il.append(InstructionConstants.AALOAD);
			// and store it in local variable 0
			il.append(_factory.createStore(Type.OBJECT, 0));
			vertexToVarIndex.put(lastOfPrevious);
		}
		
		// Create the Nodes of G_i
		createGiNodes(nodes, il, vertexToVarIndex);
		
		// retrieve the targets of outgoing tree edges
		TreeNeighborMap treeNeighbors = new TreeNeighborMap(nodes,_graph);
		createTreeNeighbors(treeNeighbors, il, vertexToVarIndex);
		
		//Now for the edges
		
		Integer sourceVertex;
		Integer targetVertex;
		int sourceIndex;
		
		if (lastOfPrevious != null){
			// create list edge from last slice to this one
			sourceVertex = lastOfPrevious;
			sourceIndex = vertexToVarIndex.get(sourceVertex);
			targetVertex = nodes.get(0);
			
			// load last node of previous slice and push it on stack
			il.append(_factory.createLoad(Type.OBJECT, sourceIndex));
			// pop the node; if this node doesn't exist, branch
			BranchInstruction ifLastOfPreviousNull = _factory.createBranchInstruction(IFNULL, null);
			il.append(ifLastOfPreviousNull);
			
			createEdge(sourceVertex, targetVertex, E1_NAME, vertexToVarIndex, il);
			
			InstructionHandle nop2 = il.append(InstructionConstants.NOP);
			ifLastOfPreviousNull.setTarget(nop2);
		}

		// Create all the list edges
		for (int j = 0; j < nodes.size()-1; j++){
			sourceVertex = nodes.get(j);
			targetVertex = nodes.get(j+1);
			
			createEdge(sourceVertex, targetVertex, E1_NAME, vertexToVarIndex, il);
		}
		
		// Create all the tree edges
		for (int j = 0; j < nodes.size(); j++){
			sourceVertex = nodes.get(j);
			targetVertex = treeNeighbors.get(sourceVertex);
			
			createEdge(sourceVertex, targetVertex, E2_NAME, vertexToVarIndex, il);
		}
		
		//TODO: remove unnecessary vertices from array, push necessary ones on array
	}
}
