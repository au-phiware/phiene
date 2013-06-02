package au.com.phiware.ga.containers;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public abstract class Diploid implements Ploid<Haploid<Diploid>> {
	@Override
	public int getNumberOfParents() {
		return 2;
	}
}
