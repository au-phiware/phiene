package au.com.phiware.ga.processes;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

import au.com.phiware.ga.Genomes;
import au.com.phiware.ga.TransformException;
import au.com.phiware.ga.Variation;
import au.com.phiware.ga.containers.Haploid;
import au.com.phiware.ga.containers.Ploid;
import au.com.phiware.ga.containers.Ploids;
import au.com.phiware.ga.io.ChromosomeOutputStream;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public abstract class Meiosis<Individual extends Ploid<Haploid<Individual>>> extends RepeatableProcess<Individual, Haploid<Individual>> implements Variation<Individual, Haploid<Individual>> {
	private Random random;
	private double crossoverFrequency = 0.002;
	protected int numberOfChromosomes = 0;

	@Override
	public int getRepeatCount(Individual individual) {
		if (numberOfChromosomes <= 0)
			numberOfChromosomes = numberOfChromosomes(individual);
		return 4 * numberOfChromosomes;
	}

	protected int numberOfChromosomes(Individual individual) {
		if (numberOfChromosomes > 0)
			return numberOfChromosomes;
		if (individual != null)
			return numberOfChromosomes = individual.getNumberOfParents();
		try {
			ParameterizedType superType = (ParameterizedType) this.getClass().getGenericSuperclass();
			while (!Meiosis.class.equals(superType.getRawType()))
				superType = (ParameterizedType) ((Class<?>) superType.getRawType()).getGenericSuperclass();
			Type[] actualType = superType.getActualTypeArguments();
			@SuppressWarnings("unchecked")
			Class<Individual> type = (Class<Individual>) actualType[0];
			return numberOfChromosomes = type.newInstance().getNumberOfParents();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numberOfChromosomes = 1;
	}

	@Override
	public boolean isTransformRepeated() {
		return true;
	}

	@Override
	public Haploid<Individual> transform(Individual ante) {
		Haploid<Individual> post = new Haploid<Individual>(ante);

		try {
			List<Haploid<Individual>> parents = Ploids.getParents(ante);
			if (!parents.isEmpty()) {
				final byte[][] heritage = new byte[parents.size()][];
				int genomeSize = 0;
				int i = 0;
				for (Haploid<Individual> parent : parents) {
					heritage[i] = parent.getGenome();
					if (genomeSize < heritage[i].length)
						genomeSize = heritage[i].length;
					i++;
				}
				final byte[] genome = new byte[genomeSize];
				
				Random random = getRandom();
				int chroma = random.nextInt(heritage.length);
				for (i = 0; i < heritage[0].length; i++) {
					if (i > 0 && random.nextDouble() < getCrossoverFrequency())
						chroma = (chroma + (heritage.length > 2 ? random.nextInt(heritage.length - 1) : 0) + 1) % heritage.length;
					genome[i] = heritage[chroma][i]; //FIXME: ensure that i is valid for heritage[chroma]
				}
				
				Genomes.setGenomeBytes(post, genome);
			} else {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				Genomes.transferGenome(ante, post, bytes, new ChromosomeOutputStream(bytes, post.getNumberOfParents()));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new TransformException(e);
		}

		return post;
	}
	
	public double getCrossoverFrequency() {
		return crossoverFrequency;
	}
	
	public void setCrossoverFrequency(double crossoverFrequency) {
		this.crossoverFrequency = crossoverFrequency;
	}

	public Random getRandom() {
		if (random == null)
			random = new Random();
		return random;
	}
	
	public String getShortName() {
		return "Meio"+super.getShortName();
	}
}
