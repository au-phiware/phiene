package au.com.phiware.ga.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

class MutationOutputStream extends FilterOutputStream {
	static double defaultMutationFrequency = 0.0001;
	private double mutationFrequency = defaultMutationFrequency;
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
	
	public double getMutationFrequency() {
		return mutationFrequency;
	}
	
	public void setMutationFrequency(double mutationFrequency) {
		this.mutationFrequency = mutationFrequency;
	}
}
