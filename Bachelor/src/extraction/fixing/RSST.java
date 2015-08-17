package extraction.fixing;

import extraction.ObjectNode;

/**
 * Route, sink, source, target.
 * This is just a wrapper.
 * @author Nixitur
 *
 */
public class RSST {

	public ObjectNode root;
	public ObjectNode sink;
	public ObjectNode source;
	public ObjectNode target;
	public RSST(){
		this(null,null,null,null);
	}
	public RSST(ObjectNode root, ObjectNode sink, ObjectNode source, ObjectNode target) {
		this.root = root;
		this.sink = sink;
		this.source = source;
		this.target = target;
	}

}
