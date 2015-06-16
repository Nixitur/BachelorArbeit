package encoding;

/**
 * An exception solely for the case that a graph is not what it should be.
 * @author Kaspar
 *
 */
public class GraphStructureException extends Exception {
	private static final long serialVersionUID = 1L;

	public GraphStructureException() {}

	public GraphStructureException(String message) {
		super(message);
	}

	public GraphStructureException(Throwable cause) {
		super(cause);
	}

	public GraphStructureException(String message, Throwable cause) {
		super(message, cause);
	}

	public GraphStructureException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
