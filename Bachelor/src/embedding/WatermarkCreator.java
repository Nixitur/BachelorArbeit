package embedding;

import org.apache.bcel.generic.*;
import org.apache.bcel.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import encoding.Encode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of this class create a Java .class file that, if the methods therein are called in a certain order,
 * constructs a specified RPG on the heap.
 * @author Kaspar
 *
 */
public class WatermarkCreator implements Constants {
	private static final String		ARRAY_NAME = "array";
	private static final String		E1_NAME = "e1";
	private static final String		E2_NAME = "e2";
	private final ObjectType 		_nodeType;
	private final String			_fullClassName;	
	private final InstructionFactory _factory;
	private final ConstantPoolGen 	_cp;
	private final ClassGen 			_cg;
	private final DirectedGraph<Integer,DefaultEdge> _graph;
	private final int				_noOfVertices;
	private final List<List<Integer>> _splitNodes;
	private final HashMap<Integer,Integer> remainingNeighbors;
	private final List<MethodGen> 	_buildMethods;
	
	private boolean manipulationAllowed = false;
	private int edgeDeleted = -1;
	
	// Each element of these lists is three InstructionHandle, the last of which holds the actual field access
	private final List<List<InstructionHandle>> _listEdgeInstructions;
	private final List<List<InstructionHandle>> _treeEdgeInstructions;
	
	/**
	 * Creates a new WaterMarkCreator.
	 * @param fullClassName The full, qualified name of the Watermark class. Integrity of name is not checked, make sure to do that beforehand or it might just crash. 
	 * @param graph The RPG that is to be embedded in the code.
	 * @param noOfSubgraphs The number of subgraphs that <code>graph</code> should be split up into. If this is larger than
	 * the number of vertices in graph, this number will be set to the number of vertices.
	 */
	public WatermarkCreator(String fullClassName, DirectedGraph<Integer,DefaultEdge> graph, int noOfSubgraphs) {
		_fullClassName = fullClassName;
		int i = fullClassName.lastIndexOf(".");
		String className = fullClassName.substring(i+1, fullClassName.length());
		_cg = new ClassGen(_fullClassName, "java.lang.Object", className+".java", ACC_PUBLIC | ACC_SUPER, new String[] {  });
		_cp = _cg.getConstantPool();
		_factory = new InstructionFactory(_cg,_cp);
		_graph = graph;
		int n = graph.vertexSet().size();
		List<Integer> hamiltonPath = encoding.Tools.hamiltonPath(n);
		_splitNodes = Tools.splitNodes(hamiltonPath, noOfSubgraphs);
		_nodeType = new ObjectType(_fullClassName);
		_noOfVertices = n;
		remainingNeighbors = noOfNeighbors();
		_listEdgeInstructions = new ArrayList<List<InstructionHandle>>(n-1);
		_treeEdgeInstructions = new ArrayList<List<InstructionHandle>>(n-2);
		// Fill the lists with null, so that they can later be set due to the erratic order in which edges are added
		Tools.fillWithNull(_listEdgeInstructions, n-1);
		Tools.fillWithNull(_treeEdgeInstructions, n-2);
		_buildMethods = new ArrayList<MethodGen>();
	}
	
	/**
	 * Creates the class and the methods contained therein, but does not actually yet add them to the class.
	 * @return The number of build methods in this class, indexed from 0 to (returnValue - 1).
	 */
	public int create(){
		createFields();
		createConstructor();
		for (int i = 0; i < _splitNodes.size(); i++){
			MethodGen method = createBuildGi(i);
			_buildMethods.add(method);
		}
//		createMain();
		manipulationAllowed = true;
		return _splitNodes.size();
	}
	
	/**
	 * Does the final stack-setting and all that jazz for the build methods. After this, no edges may be deleted.
	 */
	private void closeOffBuildMethods(){
		for (int i = 0; i < _buildMethods.size(); i++){
			MethodGen method = _buildMethods.get(i);
			method.removeNOPs();
			method.setMaxStack();
			method.setMaxLocals();
			_cg.addMethod(method.getMethod());
			method.getInstructionList().dispose();
		}
		manipulationAllowed = false;
	}
	
	/**
	 * Deletes the list edge from the vertex i to the vertex i-1. The indices go from 1 to n-1 where n is the number of vertices in the graph.
	 * Warning: If you flip a vertex' edges and also delete one of those edges, the flipping must come first.
	 * @param i The index of the vertex.
	 * @throws IllegalStateException If the class has not yet been created or the class has already been dumped.
	 */
	public void deleteListEdge(int i) throws IllegalStateException{
		if (!manipulationAllowed){
			throw new IllegalStateException("Either the class has not been created or it was already dumped. Either way, no edges can be deleted.");
		}
		i = i-1;
		List<InstructionHandle> listEdge = _listEdgeInstructions.get(i);
		for (InstructionHandle handle : listEdge){
			// Just set those three instructions to NOP. Those will be removed when the methods are closed afterwards.
			handle.setInstruction(InstructionFactory.NOP);
		}
		if (!listEdge.isEmpty()){
			edgeDeleted = i;
		}		
	}
	
	/**
	 * Deletes the tree edge originating from the vertex i. The indices go from 1 to n-2 where n is the number of vertices in the graph because
	 * the root vertex does not have a tree edge. Warning: If you flip a vertex' edges and also delete one of those edges, the flipping must come first.
	 * @param i The index of the vertex.
	 * @throws IllegalStateException If the class has not yet been created or the class has already been dumped.
	 */
	public void deleteTreeEdge(int i) throws IllegalStateException{
		if (!manipulationAllowed){
			throw new IllegalStateException("Either the class has not been created or it was already dumped. Either way, no edges can be deleted.");
		}
		i = i-1;
		List<InstructionHandle> treeEdge = _treeEdgeInstructions.get(i);
		for (InstructionHandle handle : treeEdge){
			handle.setInstruction(InstructionFactory.NOP);
		}
		if (!treeEdge.isEmpty()){
			edgeDeleted = i;
		}
	}
	
	/**
	 * Flips the edges of a specific vertex which in this case means swapping the field access instructions that set the list edge and tree edge field of a specific vertex.
	 * Warning: If you flip a vertex' edges and also delete one of those edges, the flipping must come first.
	 * @param i The index of the vertex.
	 * @throws IllegalStateException If the class has not yet been created, if the class has already been dumped, if the edges of this specific vertex have not been
	 *    set up right or if one of the edges has already been deleted.
	 */
	public void edgeFlip(int i) throws IllegalStateException{
		if (!manipulationAllowed){
			throw new IllegalStateException("Either the class has not been created or it was already dumped. Either way, no edges can be flipped.");
		}
		i = i-1;
		if (edgeDeleted == i){
			throw new IllegalStateException("One of the edges of the vertex "+(i+1)+" has already been deleted, so you may not flip those edges.");
		}
		List<InstructionHandle> treeEdgeInstructions = _treeEdgeInstructions.get(i);
		List<InstructionHandle> listEdgeInstructions = _listEdgeInstructions.get(i);
		if ((treeEdgeInstructions == null) || (listEdgeInstructions == null)){
			throw new IllegalStateException("The vertex is missing an edge.");
		}
		if ((treeEdgeInstructions.size() != 3) || (listEdgeInstructions.size() != 3)){
			throw new IllegalStateException("There should be only three InstructionHandles per edge.");
		}
		// We only swap the last one, not all of them. If we swapped all of them, we'd have to adjust the local variable indices. 
		InstructionHandle treeEdge = treeEdgeInstructions.get(2);
		InstructionHandle listEdge = listEdgeInstructions.get(2);
		Instruction treeEdgeInstruction = treeEdge.getInstruction();
		Instruction listEdgeInstruction = listEdge.getInstruction();
		treeEdge.setInstruction(listEdgeInstruction);
		listEdge.setInstruction(treeEdgeInstruction);
	}
	
	/**
	 * Adds the methods to the class and outputs the class to the correct location. <tt>create()</tt> and whatever edge deletions are wanted must be called first.
	 * @throws IOException If the class can not be saved.
	 * @throws IllegalStateException If the class has not yet been created, in which case it may not be dumped.
	 */
	public void dump(String classPath) throws IOException, IllegalStateException{
		if (!manipulationAllowed){
			throw new IllegalStateException("The class has not yet been created, so it may not be dumped yet.");
		}
		closeOffBuildMethods();
		String separator = File.separator;
		int i = _fullClassName.lastIndexOf('.');
		String packages = _fullClassName.substring(0, i).replace('.', separator.charAt(0));
		String className = _fullClassName.substring(i+1, _fullClassName.length());
		String path = classPath+separator+packages;
		File folder = new File(path);
		folder.mkdirs();
		File file = new File(path+separator+className+".class");
		FileOutputStream out = new FileOutputStream(file);
		_cg.getJavaClass().dump(out);
	}
	
	/**
	 * Returns the fully qualified class name, with packages and everything.
	 * @return The class name.
	 */
	public String getClassName(){
		return _fullClassName;
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
		return _factory.createFieldAccess(_fullClassName, ARRAY_NAME, new ArrayType(_nodeType, 1), GETSTATIC);
	}
	
	/**
	 * Creates a new Watermark instance and pushes it on the stack, ready to be stored.
	 * @param il The InstructionList that the instructions to create a new node is added to.
	 */
	private void createNewNode(InstructionList il){
		// create a brand new node and push it on stack
		il.append(_factory.createNew(_fullClassName));
		// duplicate the object on the stack
		il.append(InstructionConstants.DUP);
		// pop one from the stack and invoke constructor on it 
		il.append(_factory.createInvoke(_fullClassName, "<init>", Type.VOID, Type.NO_ARGS, INVOKESPECIAL));
	}
	
	/**
	 * Creates code that loads two Watermark instances and connects them via field assignment. This is equivalent to creating an edge.
	 * For example, if
	 * the instance that stands for <code>sourceVertex</code> is <code>o1</code>  and the instance that stands for
	 * <code>targetVertex</code> is <code>o2</code>, the assignment is<br>
	 * <code>o1.fieldName = o2</code><br> 
	 * @param sourceVertex The source vertex of the edge.
	 * @param targetVertex The target vertex of the edge.
	 * @param fieldName The field name on the instance that stands for <code>sourceVertex</code> that is to be set to the instance that stands
	 *   for <code>targetVertex</code>
	 * @param vertexToVarIndex Assigns to each vertex a local variable index.
	 * @param il The instruction list that is appended to by this method.
	 * @return The InstructionHandles for this add-edge section. 
	 */
	private List<InstructionHandle> createEdge(Integer sourceVertex, Integer targetVertex, String fieldName, VertexToVarIndexMap vertexToVarIndex, 
			InstructionList il){
		List<InstructionHandle> edgeInstructions = new ArrayList<InstructionHandle>();
		int sourceIndex = vertexToVarIndex.get(sourceVertex).intValue();
		int targetIndex = vertexToVarIndex.get(targetVertex).intValue();
		
		InstructionHandle ins;
		ins = il.append(InstructionFactory.createLoad(Type.OBJECT, sourceIndex));
		edgeInstructions.add(ins);
		ins = il.append(InstructionFactory.createLoad(Type.OBJECT, targetIndex));
		edgeInstructions.add(ins);
		ins = il.append(_factory.createFieldAccess(_fullClassName, fieldName, _nodeType, PUTFIELD));
		edgeInstructions.add(ins);
		// reduce the count of how many neighbors are remaining for both nodes
		reduceRemainingNeighbors(sourceVertex,targetVertex);
		return edgeInstructions;
	}
	
	/**
	 * Creates code that creates Watermark instances that stand for the nodes in <code>nodes</code>.
	 * @param nodes The nodes that are to be turned into code.
	 * @param il The instruction list that is appended to by this method.
	 * @param vertexToVarIndex Assigns to each vertex a local variable index.
	 */
	private void createGiNodes(List<Integer> nodes, InstructionList il, VertexToVarIndexMap vertexToVarIndex){
		for (int j = 0; j < nodes.size(); j++){
			createNewNode(il);
			// pop node from stack and store it in local variable
			il.append(InstructionFactory.createStore(Type.OBJECT, vertexToVarIndex.getLocalVarIndex()));
			vertexToVarIndex.put(nodes.get(j));
		}
	}
	
	/**
	 * Creates code that extracts the necessary incoming neighbors via tree edges of specified nodes
	 *  
	 * @param treeNeighbors
	 * @param il The <code>InstructionList</code> that is to be appended to.
	 * @param vertexToVarIndex Assigns to each vertex a local variable index.
	 */
	private void createTreeNeighbors(TreeNeighborMap treeNeighbors, InstructionList il, VertexToVarIndexMap vertexToVarIndex){
		List<Integer> inNeighbors;
		for (Integer vertex : treeNeighbors.keySet()){
			// Get all inNeighbors via tree edges of vertex
			inNeighbors = treeNeighbors.get(vertex);
			for (Integer inNeighbor : inNeighbors){
				// if this inNeighbor of vertex hasn't been stored yet
				if (vertexToVarIndex.get(inNeighbor) == null){
					createLoadFromArray(il,inNeighbor,vertexToVarIndex);
				}
			}
		}
	}
	
	/**
	 * Creates code that checks if the entry in the array at a specified index is <tt>null</tt>. If it is, the code
	 * creates a new Watermark instance. If it isn't, that entry is loaded. Whatever the case, the resulting Watermark
	 * is then stored. Essentially, it creates<br>
	 * <tt>n_arrayIndex = (array[arrayIndex] == null) ? new Watermark() : array[arrayIndex];</tt>
	 * @param il The InstructionList that is to be appended to.
	 * @param arrayIndex The specified index.
	 * @param vertexToVarIndex Assigns to each vertex a local variable index.
	 */
	private void createLoadFromArray(InstructionList il, int arrayIndex, VertexToVarIndexMap vertexToVarIndex){
		il.append(getArray());
		il.append(new PUSH(_cp, arrayIndex));
		il.append(InstructionConstants.AALOAD);
		  BranchInstruction ifArrayEntryNonNull = InstructionFactory.createBranchInstruction(Constants.IFNONNULL,null);
		il.append(ifArrayEntryNonNull);
		// the following is if it IS null
		createNewNode(il);
		  BranchInstruction gotoEnd = InstructionFactory.createBranchInstruction(Constants.GOTO, null);
		il.append(gotoEnd);
		// the following is if it is NOT null
		InstructionHandle notNullBranch = il.append(getArray());
		ifArrayEntryNonNull.setTarget(notNullBranch);
		il.append(new PUSH(_cp, arrayIndex));
		il.append(InstructionConstants.AALOAD);
		InstructionHandle end = il.append(InstructionFactory.createStore(Type.OBJECT, vertexToVarIndex.getLocalVarIndex()));
		vertexToVarIndex.put(arrayIndex);
		gotoEnd.setTarget(end);
	}
	
	/**
	 * Creates code that checks whether <code>array</code> is null. If it is, a new array is built and filled with
	 * <tt>null</tt>
	 * @param il The <code>InstructionList</code> that is to be appended to.
	 */
	private void createCheckIfArrayNull(InstructionList il){
		// push array on stack
		il.append(getArray());
		// check if top of stack is not null; if so, go to an as of yet undetermined (null) instruction
		BranchInstruction ifArrayNonNull = InstructionFactory.createBranchInstruction(IFNONNULL, null);
		il.append(ifArrayNonNull);
		// push the number of vertices on the stack
		il.append(new PUSH(_cp, _noOfVertices-1));
		// push new array (with 1 dimension and length on top of stack) on stack
		il.append(_factory.createNewArray(_nodeType, (short) 1));
		// take what is on top of the stack and save it as the field ARRAY_NAME
		il.append(_factory.createFieldAccess(_fullClassName, ARRAY_NAME, new ArrayType(_nodeType,1), PUTSTATIC));
		
		// Just wait for a bit
		InstructionHandle nop = il.append(InstructionConstants.NOP);
		// jump to this instruction if array was not null
		ifArrayNonNull.setTarget(nop);
	}

	/**
	 * Creates the fields of the class and adds them to the class generator.
	 */
	private void createFields(){
		FieldGen field;
		
		field = new FieldGen(ACC_PUBLIC, _nodeType, E1_NAME, _cp);
		_cg.addField(field.getField());
		
		field = new FieldGen(ACC_PUBLIC, _nodeType, E2_NAME, _cp);
		_cg.addField(field.getField());
		
		field = new FieldGen(ACC_PUBLIC | ACC_STATIC, new ArrayType(_nodeType, 1), ARRAY_NAME, _cp);
		_cg.addField(field.getField());
	}
	
	/**
	 * Creates the constructor for the class. This constructor has no arguments and does literally nothing except create a new instance.
	 */
	private void createConstructor(){
		InstructionList il = new InstructionList();
		// Constructor is public, has no return type, no arguments, is called <init> and is in Watermark
		MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {}, "<init>", _fullClassName, il, _cp);
		
		// push the name of Object class on stack
		il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
		// invoke <init> on class that is on the stack
		il.append(_factory.createInvoke("java.lang.Object", "<init>", Type.VOID, Type.NO_ARGS, INVOKESPECIAL));
		// return from the methd
		il.append(InstructionFactory.createReturn(Type.VOID));
		method.setMaxStack();
		method.setMaxLocals();
		_cg.addMethod(method.getMethod());
		il.dispose();
	}
	
//	/**
//	 * Creates a <code>public static void main(String[] args)</code> method that calls all buildG# methods in order.
//	 * This is solely for testing, don't actually leave this in. 
//	 */
//	private void createMain(){
//		InstructionList il = new InstructionList();
//		MethodGen method = new MethodGen(ACC_PUBLIC | ACC_STATIC, Type.VOID, new Type[] { new ArrayType(Type.STRING, 1) }, new String[] { "arg0" }, "main", _fullClassName, il, _cp);
//		
//		for (int i = 0; i < _splitNodes.size(); i++){
//			il.append(_factory.createInvoke(_fullClassName, "buildG"+i, Type.VOID, Type.NO_ARGS, Constants.INVOKESTATIC));
//		}
//		il.append(InstructionFactory.createReturn(Type.VOID));
//		method.setMaxStack();
//	    method.setMaxLocals();
//	    _cg.addMethod(method.getMethod());
//	    il.dispose();
//	}
	
	/**
	 * Creates code that builds the <code>i</code>-th subgraph. 
	 * @param i The index of the subgraph that is to be created. It goes from 0 to <code>noOfSubgraphs-1</code>.
	 */
	private MethodGen createBuildGi(int i){
		List<Integer> nodes = _splitNodes.get(i);
		boolean thisIsG0 = (i == 0);
		Integer lastNodeInGi = nodes.get(nodes.size()-1);
		Integer firstNodeInPrevious = null;
		// if this is not G0, the leftmost subgraph with the smallest indices
		if (!thisIsG0){
			firstNodeInPrevious = lastNodeInGi-1;
		}
		
		InstructionList il = new InstructionList();
		MethodGen method = new MethodGen(ACC_PUBLIC | ACC_STATIC, Type.VOID, Type.NO_ARGS, new String[] {}, "buildG"+i, _fullClassName, il, _cp);
		
		createCheckIfArrayNull(il);
		
		// Keep track of the indices that all our local variables have
		VertexToVarIndexMap vertexToVarIndex = new VertexToVarIndexMap();

		// If this is not G_0
		if (!thisIsG0){
			this.createLoadFromArray(il, firstNodeInPrevious, vertexToVarIndex);
		}
		
		// Create the Nodes of G_i
		createGiNodes(nodes, il, vertexToVarIndex);
		
		// retrieve the targets of outgoing tree edges
		TreeNeighborMap treeNeighbors = new TreeNeighborMap(nodes,_graph);
		createTreeNeighbors(treeNeighbors, il, vertexToVarIndex);
		
		//Now for the edges
		
		Integer sourceVertex;
		Integer targetVertex;
		
		// create list edge from this one to last one
		if (!thisIsG0){
			sourceVertex = lastNodeInGi;
			targetVertex = firstNodeInPrevious;
			
			List<InstructionHandle> listEdge = createEdge(sourceVertex, targetVertex, E1_NAME, vertexToVarIndex, il);
			_listEdgeInstructions.set(sourceVertex, listEdge);
		}

		// Create all the list edges
		for (int j = 0; j < nodes.size()-1; j++){
			sourceVertex = nodes.get(j);
			targetVertex = nodes.get(j+1);
			
			List<InstructionHandle> listEdge = createEdge(sourceVertex, targetVertex, E1_NAME, vertexToVarIndex, il);
			_listEdgeInstructions.set(sourceVertex, listEdge);
		}
		
		// Create all the tree edges
		for (int j = 0; j < nodes.size(); j++){
			// get the target node in nodes
			targetVertex = nodes.get(j);
			// go through all its inNeighbors via tree edges
			for (Integer vertex : treeNeighbors.get(targetVertex)){
				sourceVertex = vertex;
				List<InstructionHandle> treeEdge = createEdge(sourceVertex, targetVertex, E2_NAME, vertexToVarIndex, il);
				_treeEdgeInstructions.set(sourceVertex, treeEdge);
			}
		}
		
		// Go through all vertices that were used in this subgraph
		Integer index;
		for (Object o : vertexToVarIndex.keySet()){
			Integer j = (Integer) o;
			if (j < 0){
				continue;
			}
			index = vertexToVarIndex.get(j);
			il.append(getArray());
			// push desired array index on stack
			il.append(new PUSH(_cp, j.intValue()));
			// If it still has neighbors remaining, e.g. if it still needs to be dealt with
			if(remainingNeighbors.get(j) > 0){					
				// push object to be stored on stack
				il.append(InstructionFactory.createLoad(Type.OBJECT, index));
			} else {
				// push null on stack
				il.append(InstructionConstants.ACONST_NULL);				
			}
			// store local var or null in array at index j
			il.append(InstructionConstants.AASTORE);
		}
		// TODO: This is just for testing
//		il.append(_factory.createPrintln("buildG"+i+" called"));
		
		il.append(InstructionFactory.createReturn(Type.VOID));
		
		
		return method;
	}
	
	public static void main(String[] args) throws IOException{
		int w = 5;
		int k = 3;
		if (args.length > 0) {
			w = Integer.parseInt(args[0]);
			if (args.length > 1) {
				k = Integer.parseInt(args[1]);
			}
		}
		DirectedGraph<Integer, DefaultEdge> graph = Encode.encodeWToRPG(w);
		
		String fullClassName = "example.Watermark";
		WatermarkCreator creator = new WatermarkCreator(fullClassName, graph, k);
		creator.create();
	}
}