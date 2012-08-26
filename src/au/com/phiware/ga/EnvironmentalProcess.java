package au.com.phiware.ga;

public interface EnvironmentalProcess<Individual extends Container> {
	public void didAddToEnvironment(Environment<Individual> e);
}
