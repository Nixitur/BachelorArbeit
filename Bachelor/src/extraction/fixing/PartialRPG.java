package extraction.fixing;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

public class PartialRPG extends SimpleDirectedGraph<Integer, DefaultEdge> {
	private static final long serialVersionUID = 1L;
	public static final int RPG_TYPE_UNBROKEN = 0;
	public static final int RPG_TYPE_MISSING_LIST_EDGE = 1;
	public static final int RPG_TYPE_MISSING_BACK_EDGE = 2;
	public static final int RPG_TYPE_MISSING_ROOT = -1;
	public int type = 0;
	public PartialRPG() {
		super(DefaultEdge.class);
	}
}
