package extraction.fixing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.DirectedSubgraph;

import util.GraphStructureException;

public class RepresentativeForest extends SimpleDirectedGraph<Integer, DefaultEdge> {
	private static final long serialVersionUID = 5337022394010877420L;
	
	// n as used in the paper of Bento et al.
	private int n;
	
	// LARGE: n < v <= 2n+1
	private Set<Integer> large;
	// SMALL: 0 < v <= n
	private Set<Integer> small;
	
	private HashMap<DirectedGraph<Integer,DefaultEdge>,Integer> treeRoots;
	private HashMap<Integer,List<Integer>> children;
	private HashMap<Integer,Set<Integer>> descendants;
	private HashMap<Integer,Integer> parent;
	// The fixed element
	private Integer f;

	public <V> RepresentativeForest(DirectedGraph<V,DefaultEdge> graph, List<V> hamiltonianPath) throws GraphStructureException{
		super(DefaultEdge.class);
		List<V> hamiltonPath = hamiltonianPath;
		if (hamiltonPath == null){
			throw new GraphStructureException("The Hamilton path of the RPG has not been fixed yet.");
		}
		large = new HashSet<Integer>();
		small = new HashSet<Integer>();
		// The highest vertex is 2n+2, the lowest one is 0
		n = (graph.vertexSet().size() - 3) / 2;
		int edgeNumber = graph.edgeSet().size();
		if (edgeNumber < 4*n+1){
			throw new GraphStructureException("There are too many missing edges to fix the graph.");
		} else if (edgeNumber > 4*n+3){
			throw new GraphStructureException("There are too many edges. How did that happen?");
		}
		boolean oneEdgeMissing = (graph.edgeSet().size() == 4*n+2);
		// This graph can now finally be correctly labeled since the Hamilton path is known. Thus, it's better to just use Integer nodes.
		HashMap<V,Integer> nodeToInteger = new HashMap<V,Integer>();
		Set<DefaultEdge> listEdges = new HashSet<DefaultEdge>();
		// The first node in the Hamilton path is the root that is identified with the vertex 2n+2
		Integer index = 2 * n + 2;
		V lastNode = null;
		for (V node : hamiltonPath){			
			if (lastNode != null){
				DefaultEdge listEdge = graph.getEdge(lastNode, node);
				listEdges.add(listEdge);
			}
			// The representative tree does not contain 0
			if (index == 0){
				break;
			}
			nodeToInteger.put(node,index);
			addVertex(index);
			if (index <= n){
				small.add(index);
			} else if (index <= 2*n+1){
				large.add(index);
			}
			index--;
			lastNode = node;
		}
		// Clone the graph's edge set, so that it is not overwritten
		Set<DefaultEdge> edgeSet = new HashSet<DefaultEdge>(graph.edgeSet());

		// remove all list edges, so that only tree edges remain; there is now no edge touching 0 anymore
		edgeSet.removeAll(listEdges);
		// edgeSet is linear in size
		for (DefaultEdge edge : edgeSet){
			V source = graph.getEdgeSource(edge);
			V target = graph.getEdgeTarget(edge);
			// Invert the edge direction
			Integer targetInt = nodeToInteger.get(source);
			Integer sourceInt = nodeToInteger.get(target);
			// If only one tree edge is missing, remove a random edge to make it two as that's what the algorithms are designed for
			addEdge(sourceInt,targetInt);
		}
		if (oneEdgeMissing){
			// If exactly one edge is missing, remove another random tree edge
			removeEdge(edgeSet.iterator().next());
		}
		// Since this is missing either zero or two edges, there should be one or three trees
		Set<DirectedGraph<Integer,DefaultEdge>> trees = GraphTools.getConnectedComponents(this);
		treeRoots = new HashMap<DirectedGraph<Integer,DefaultEdge>,Integer>();
		for (DirectedGraph<Integer,DefaultEdge> tree : trees){
			Integer treeRoot = Collections.max(tree.vertexSet());
			treeRoots.put(tree,treeRoot);
		}
		setChildrenAndParents();
		// If there are no missing edges, setting descendants is superfluous and takes way too long
		if(oneEdgeMissing){
			setDescendants();
		}
		f = getFixedElement();
	}
	
	/**
	 * Returns the root children in the original representative tree without removed edges.
	 * @return The children of the root node.
	 */
	public Set<Integer> getRootChildren(){
		Set<Integer> rootChildren = new HashSet<Integer>(children.get(2*n+2));
		if (f == 2*n+1){
			// if f==2n+1, then the children of the root are simply {n+1,n+2,...,2n+1}, all the large vertices
			return large;
		}
		Set<Integer> xC = new HashSet<Integer>(large);
		xC.remove(f);
		xC.add(2*n+2);
		DirectedGraph<Integer,DefaultEdge> inducedXcGraph = new DirectedSubgraph<Integer,DefaultEdge>(this,xC,null);
		Set<DirectedGraph<Integer,DefaultEdge>> components = GraphTools.getConnectedComponents(inducedXcGraph);
		// if F[Xc] connected...
		// This is especially the case if there are no missing edges; that's why descendants aren't necessary in that case
		if (components.size() == 1){			
			return rootChildren;
		}
		Set<Integer> isolatedVertices = new HashSet<Integer>();
		for (DirectedGraph<Integer,DefaultEdge> tree : treeRoots.keySet()){
			Set<Integer> vertices = tree.vertexSet();
			if (vertices.size() == 1){
				isolatedVertices.add(vertices.iterator().next());
			}
		}
		if (isolatedVertices.size() == 0){
			rootChildren.add(2*n+1);
			return rootChildren;
		}
		if (isolatedVertices.size() == 2){
			rootChildren.addAll(isolatedVertices);
			return rootChildren;
		}
		// There can be no more than 2 isolated vertices since we only removed two edges. Hence, we don't need to check for size() == 1
		Integer x = isolatedVertices.iterator().next();
		if (descendants.get(f).size() != 2*n-f+1){
			rootChildren.add(x);
			return rootChildren;
		}
		Deque<Integer> preord = preorderTraversal(f);
		Integer yR = preord.getLast();
		if (rootChildren.size() < yR){
			rootChildren.add(x);
			rootChildren.add(2*n+1);
		}
		return rootChildren;
	}
	
	/**
	 * Sets the children and parent of each and every vertex in ascending order. This is DEFINITELY not O(n) due to the sorting, but what can you do.
	 */
	private void setChildrenAndParents(){
		children = new HashMap<Integer,List<Integer>>();
		parent = new HashMap<Integer,Integer>();
		for (Integer v : vertexSet()){
			List<Integer> vChildren = new ArrayList<Integer>();
			children.put(v, vChildren);
		}
		for (DefaultEdge e : edgeSet()){
			Integer source = getEdgeSource(e);
			Integer target = getEdgeTarget(e);
			children.get(source).add(target);
			parent.put(target, source);
		}
		for (List<Integer> vChildren : children.values()){
			Collections.sort(vChildren);
		}
	}
	
	/**
	 * Calculates the descendants of every vertex and stores them in descendants.
	 */
	private void setDescendants(){
		descendants = new HashMap<Integer,Set<Integer>>();
		for (Integer root : treeRoots.values()){
			getDescendants(root);
		}
		return;
	}
	
	/**
	 * Gets the descendants of a specific vertex and also stores it in the descendants field. This is DEFINITELY not O(n) due to all the addAll() calls, but what can you do.
	 * @param v a vertex in the graph
	 * @return The set of descendant vertices of v
	 */
	private Set<Integer> getDescendants(Integer v){
		Set<Integer> vDescendants = new HashSet<Integer>();
		List<Integer> vChildren = children.get(v);
		if (vChildren.isEmpty()){
			Set<Integer> empty = new HashSet<Integer>();
			descendants.put(v, empty);
			return empty;
		}
		for (Integer child : vChildren){
			vDescendants.add(child);
			vDescendants.addAll(getDescendants(child));
		}
		descendants.put(v, vDescendants);
		return vDescendants;
	}
	
	/**
	 * Calculates the fixed element of a broken RPG.
	 * @return The fixed element.
	 * @throws GraphStructureException If no fixed element can be found which implies that it's not actually an RPG.
	 */
	private Integer getFixedElement() throws GraphStructureException{
		if(is2n1FixedElement()){
			return 2*n+1;
		}
		Set<Integer> largeCopy = new HashSet<Integer>(large);
		// If the forest has a large vertex x, excluding the children of 2n+2...
		largeCopy.removeAll(children.get(2*n+2));
		for (Integer x : largeCopy){
			// with a sibling z...
			List<Integer> siblings = children.get(parent.get(x));
			if (siblings.size() > 1){
				// then f is max(x,z)
				return Collections.max(siblings);
			}
		}
		for (Integer x : largeCopy){
			if (children.get(x).isEmpty()){
				continue;
			}
			Set<Integer> ySharp = new HashSet<Integer>();
			// Y' = {x-n, x-n+1, ..., n}
			for (int i = x-n; i <= n; i++){
				ySharp.add(i);
			}
			// if N*_F(x) contained in Y'
			Set<Integer> xDescendants = descendants.get(x);
			if (ySharp.containsAll(xDescendants)){
				// If N*_F(x) == Y'
				if (ySharp.size() == xDescendants.size()){
					return x;
				}
				// or if Y' \ N*_F(x)...
				// TODO: I'm PRETTY sure there's a typo in the paper. It says N+_F(x) there, but the theorem states that Y' must be partitioned into N*_F(x) and such a Y'2 that is the vertex set of one of the trees
				ySharp.removeAll(xDescendants);
				for(DirectedGraph<Integer,DefaultEdge> tree : treeRoots.keySet()){
					// is the vertex set of one of the trees
					if (tree.vertexSet().equals(ySharp)){
						return x;
					}
				}
			}
		}
		for (DirectedGraph<Integer,DefaultEdge> tree : treeRoots.keySet()){
			Deque<Integer> preorder = preorderTraversal(treeRoots.get(tree));
			Integer rightmost = preorder.getLast();
			if (large.contains(rightmost)){
				return rightmost;
			}
		}
		throw new GraphStructureException("No fixed element found.");
	}
	
	/**
	 * Gets the preorder traversal of the forest, starting from a specific vertex
	 * @param root The vertex at which to start
	 * @return The preorder traversal.
	 */
	private Deque<Integer> preorderTraversal(Integer root){
		Deque<Integer> result = new LinkedList<Integer>();
		result.add(root);
		List<Integer> rootChildren = children.get(root);
		if (rootChildren.isEmpty()){			
			return result;
		}
		for (Integer child : rootChildren){
			Deque<Integer> childTraversal = preorderTraversal(child);
			result.addAll(childTraversal);
		}
		return result;
	}
	
	/**
	 * Whether 2n+1 is the fixed element of this graph. Set children first.
	 * @return <tt>true</tt> if 2n+1 is the fixed element of this graph. <tt>false</tt> otherwise.
	 */
	private boolean is2n1FixedElement(){
		// The fixed element is 2n+1 if and only if 2n+1 is a leaf...
		if (!children.get(2*n+1).isEmpty()){
			return false;
		}
		// AND all small vertices are children of 2n...
		Set<Integer> smallCopy = new HashSet<Integer>(small);
		List<Integer> childrenOf2n = children.get(2*n);
		// with the possible exception of at most 2 of them...
		smallCopy.removeAll(childrenOf2n);
		if(smallCopy.size() > 2){
			return false;
		}		
		for(Integer v : smallCopy){
			// in which case those exceptions must be isolated
			if (!edgesOf(v).isEmpty()){
				return false;
			}
		}
		return true;
	}
}
// TODO: Currently, it seems as if setting up the tree (i.e. sorting the children of each vertex to get proper ascending order and getting the descendants of each vertex)
// CANNOT be performed in linear time. I get the feeling that I'm missing something, but especially the former seems kind of impossible.