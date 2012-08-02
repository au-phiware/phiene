package au.com.phiware.ga.containers;

public abstract class Diploid extends Polyploid<Haploid<Diploid>> {
	@Override
	public int getNumberOfParents() {
		return 2;
	}
}
