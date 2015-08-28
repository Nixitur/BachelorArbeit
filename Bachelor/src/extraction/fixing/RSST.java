package extraction.fixing;

/**
 * Route, sink, source, target.
 * This is just a wrapper.
 * @author Nixitur
 *
 */
public class RSST {

	public Integer root;
	public Integer sink;
	public Integer source;
	public Integer target;
	public RSST(){
		this(null,null,null,null);
	}
	public RSST(Integer root, Integer sink, Integer source, Integer target) {
		this.root = root;
		this.sink = sink;
		this.source = source;
		this.target = target;
	}

}
