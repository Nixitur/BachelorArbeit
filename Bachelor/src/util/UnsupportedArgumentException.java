package util;

public class UnsupportedArgumentException extends Exception {
	private static final long serialVersionUID = 1L;
	private static final String errorString = "Calls to mark may only either "
							+ "have no argument or one argument of type int or java.lang.String: ";

	public UnsupportedArgumentException() {
		super(errorString);
	}

	public UnsupportedArgumentException(String arg0) {
		super(errorString+arg0);
	}

	public UnsupportedArgumentException(Throwable arg0) {
		super(arg0);
	}

	public UnsupportedArgumentException(String arg0, Throwable arg1) {
		super(errorString+arg0, arg1);
	}

	public UnsupportedArgumentException(String arg0, Throwable arg1,
			boolean arg2, boolean arg3) {
		super(errorString+arg0, arg1, arg2, arg3);
	}

}
