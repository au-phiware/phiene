package au.com.phiware.ga;

public class EvolutionTransformException extends RuntimeException {
	private static final long serialVersionUID = 8630990452096723074L;
	private Container individual;

	public EvolutionTransformException() {
		this((Container) null);
	}

	public EvolutionTransformException(String message) {
		this(message, (Container) null);
	}

	public EvolutionTransformException(Throwable cause) {
		this(cause, null);
	}

	public EvolutionTransformException(String message, Throwable cause) {
		this(message, cause, null);
	}

	public EvolutionTransformException(Container individual) {
		super();
		this.individual = individual;
	}

	public EvolutionTransformException(String message, Container individual) {
		super(message);
		this.individual = individual;
	}

	public EvolutionTransformException(Throwable cause, Container individual) {
		super(cause);
		this.individual = individual;
	}

	public EvolutionTransformException(String message, Throwable cause, Container individual) {
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
