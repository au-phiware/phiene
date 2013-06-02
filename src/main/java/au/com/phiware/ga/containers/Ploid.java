package au.com.phiware.ga.containers;

import au.com.phiware.ga.Container;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface Ploid<Parent extends Ploid<?>> extends Container {
	public abstract int getNumberOfParents();
}
