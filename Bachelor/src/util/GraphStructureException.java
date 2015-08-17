package util;

/**
 * An exception solely for the case that a graph is not what it should be.
 * @author Kaspar
 *
 */
public class GraphStructureException extends Exception {
	private static final long serialVersionUID = 1L;
	private static final String errorString = "The graph is not a reducible permutation graph.";

	public GraphStructureException() {
		super(errorString);
	}

	public GraphStructureException(String message) {
		super(errorString+": "+message);
	}

	public GraphStructureException(Throwable cause) {
		super(cause);
	}

	public GraphStructureException(String message, Throwable cause) {
		super(errorString+": "+message, cause);
	}

	public GraphStructureException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(errorString+": "+message, cause, enableSuppression, writableStackTrace);
	}

}
