package encoding;

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