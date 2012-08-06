package au.com.phiware.ga.containers;

public abstract class Diploid implements Ploid<Haploid<Diploid>> {
	@Override
	public int getNumberOfParents() {
		return 2;
	}
}
