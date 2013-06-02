package au.com.phiware.ga;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface EnvironmentalProcess<Individual extends Container> {
	public void didAddToEnvironment(Environment<Individual> e);
}
