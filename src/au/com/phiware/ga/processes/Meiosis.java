package au.com.phiware.ga.processes;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Random;

import au.com.phiware.ga.TransformException;
import au.com.phiware.ga.Genomes;
import au.com.phiware.ga.Variation;
import au.com.phiware.ga.containers.Haploid;
import au.com.phiware.ga.containers.Ploid;
import au.com.phiware.ga.io.MutationOutputStream;

public abstract class Meiosis<Individual extends Ploid<?>> extends Variation<Individual, Haploid<Individual>> {
	private class CrossoverOutputStream extends au.com.phiware.ga.io.CrossoverOutputStream {
		CrossoverOutputStream(OutputStream out) {
			super(out, numberOfChromosomes(null));
		}
		
		public double getCrossoverFrequency() {
			return Meiosis.this.getCrossoverFrequency();
		}
		
		public void setCrossoverFrequency(double crossoverFrequency) {
			Meiosis.this.setCrossoverFrequency(crossoverFrequency);
		}
	}

	private Random random;
	private double crossoverFrequency;
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
			@SuppressWarnings({ "unchecked", "unused" })
			OutputStream[] chain = Genomes.getGenomeFilters(ante, post,
					CrossoverOutputStream.class, MutationOutputStream.class);
		} catch (IOException e) {
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

}
