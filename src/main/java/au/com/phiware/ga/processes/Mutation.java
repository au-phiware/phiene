package au.com.phiware.ga.processes;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import au.com.phiware.ga.AbstractProcess;
import au.com.phiware.ga.Container;
import au.com.phiware.ga.Genomes;
import au.com.phiware.ga.TransformException;
import au.com.phiware.ga.io.MutationOutputStream;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public class Mutation<Individual extends Container> extends
                AbstractProcess<Individual, Individual> {
	@Override
	public Individual transform(Individual individual) {
		try {
			OutputStream[] chain = Genomes.getGenomeFilters(individual, MutationOutputStream.class);
			ByteArrayOutputStream bytes = (ByteArrayOutputStream) chain[0];
			MutationOutputStream mutator = (MutationOutputStream) chain[1];
			
			if (mutator.getMutationCount() > 0)
				Genomes.setGenomeBytes(individual, bytes.toByteArray());
		} catch (Exception e) {
			throw new TransformException(e);
		}
		return individual;
	}

	@Override
	public String getShortName() {
		return "Muta"+super.getShortName();
	}
}
