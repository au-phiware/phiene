package au.com.phiware.ga;

import java.util.Collection;

public abstract class Process {

	public abstract <Individual extends Container> Collection<Individual> transform(Collection<Individual> population) throws EvolutionTransformException;
	
}
