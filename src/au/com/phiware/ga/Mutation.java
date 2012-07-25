package au.com.phiware.ga;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class Mutation extends Variation<Container, Container> {
	private static double mutationFrequency = 0.0001;
	
	static class MutationOutputStream extends FilterOutputStream {
		private Random random = new Random();
		private int mutationCount;
		
		public MutationOutputStream(OutputStream out) {
			super(out);
		}
		
		/**
		 * @return the number of mutations performed by this MutationOutputStream
		 */
		public int getMutationCount() {
			return mutationCount;
		}

		@Override
		public void write(int b) throws IOException {
			if (random.nextDouble() < getMutationFrequency()) {
				b ^= 1 << (random.nextInt() & 7);
				++mutationCount;
			}
			out.write(b);
		}
	}
	
	public static double getMutationFrequency() {
		return mutationFrequency;
	}

	public static void setMutationFrequency(double mutationFrequency) {
		Mutation.mutationFrequency = mutationFrequency;
	}

	@Override
	public Container transform(Container individual) {
		try {
			@SuppressWarnings("unchecked")
			OutputStream[] chain = Genomes.getGenomeFilters(individual, MutationOutputStream.class);
			ByteArrayOutputStream bytes = (ByteArrayOutputStream) chain[0];
			MutationOutputStream mutator = (MutationOutputStream) chain[1];
			
			if (mutator.getMutationCount() > 0)
				Genomes.setGenomeBytes(individual, bytes.toByteArray());
		} catch (IOException e) {
			throw new EvolutionTransformException(e);
		}
		return individual;
	}
}
