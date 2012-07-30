package au.com.phiware.ga;

public class TransformException extends RuntimeException {
	private static final long serialVersionUID = 8630990452096723074L;
	private Container individual;

	public TransformException() {
		this((Container) null);
	}

	public TransformException(String message) {
		this(message, (Container) null);
	}

	public TransformException(Throwable cause) {
		this(cause, null);
	}

	public TransformException(String message, Throwable cause) {
		this(message, cause, null);
	}

	public TransformException(Container individual) {
		super();
		this.individual = individual;
	}

	public TransformException(String message, Container individual) {
		super(message);
		this.individual = individual;
	}

	public TransformException(Throwable cause, Container individual) {
		super(cause);
		this.individual = individual;
	}

	public TransformException(String message, Throwable cause, Container individual) {
		super(message, cause);
		this.individual = individual;
	}

	public Container getIndividual() {
		return individual;
	}

	public void setIndividual(Container individual) {
		this.individual = individual;
	}

}
