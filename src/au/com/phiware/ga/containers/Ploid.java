package au.com.phiware.ga.containers;

import au.com.phiware.ga.Container;

public interface Ploid<Parent extends Ploid<?>> extends Container {
	public abstract int getNumberOfParents();
}
