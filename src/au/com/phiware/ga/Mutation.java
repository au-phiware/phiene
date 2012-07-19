package au.com.phiware.ga;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Random;

public class Mutation extends Variation {
	private static double mutationFrequency = 0.0001;
	
	static class MutationOutputStream extends FilterOutputStream {
		private Random random = new Random();
		private ByteArrayOutputStream bytes;
		private int mutationCount;
		
		public MutationOutputStream(OutputStream out) {
			super(out);
			if (this.out == null)
				this.out = this.bytes = new ByteArrayOutputStream();
			else {
				if (this.out instanceof ByteArrayOutputStream)
					this.bytes = (ByteArrayOutputStream) this.out;
				else
					this.bytes = new ByteArrayOutputStream();
			}
		}
		
		/**
		 * @return the number of mutations performed by this MutationOutputStream
		 */
		public int getMutationCount() {
			return mutationCount;
		}

		/**
		 * @return the bytes
		 */
		public byte[] toByteArray() {
			return bytes.toByteArray();
		}

		@Override
		public void write(int b) throws IOException {
			if (random.nextDouble() < getMutationFrequency()) {
				b ^= 1 << (random.nextInt() & 7);
				++mutationCount;
			}
			out.write(b);
			if (bytes != out)
				bytes.write(b);
		}
	}
	
	public static double getMutationFrequency() {
		return mutationFrequency;
	}

	public static void setMutationFrequency(double mutationFrequency) {
		Mutation.mutationFrequency = mutationFrequency;
	}

	@Override
	public <Individual extends Container> Collection<Individual> transform(
			Collection<Individual> population)
			throws EvolutionTransformException {
		
		for (Individual individual : population) {
			try {
				MutationOutputStream mutator = Genomes.getGenomeFilter(individual, MutationOutputStream.class);
				if (mutator.getMutationCount() > 0)
					Genomes.setGenomeBytes(individual, mutator.toByteArray());
			} catch (IOException e) {
				throw new EvolutionTransformException(e);
			}
		}
		
		return population;
	}
}
