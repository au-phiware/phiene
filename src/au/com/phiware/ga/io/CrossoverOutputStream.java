package au.com.phiware.ga.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public abstract class CrossoverOutputStream extends FilterOutputStream {
	private double crossoverFrequency;
	private Random random;
	private int crossoverCount;
	private int cursor;
	private final int size;
	private int step;
	
	protected CrossoverOutputStream(OutputStream out, int size) {
		this(out, size, 0.01, new Random());
	}
	
	protected CrossoverOutputStream(OutputStream out, int size, double crossoverFrequency) {
		this(out, size, crossoverFrequency, new Random());
	}
	
	protected CrossoverOutputStream(OutputStream out, int size, double crossoverFrequency, Random random) {
		super(out);
		this.size = size;
		this.crossoverFrequency = crossoverFrequency;
		this.random = random;
		step = random.nextInt(size);
	}
	
	/**
	 * @return the number of mutations performed by this MutationOutputStream
	 */
	public int getCrossoverCount() {
		return crossoverCount;
	}

	@Override
	public void write(int b) throws IOException {
		if (cursor > 0 && cursor % size == 0 && random.nextDouble() < getCrossoverFrequency()) {
			crossoverCount++;
			step = (step + (size > 2 ? random.nextInt(size - 1) : 0) + 1) % size;
		}
			
		if (cursor++ % size == step)
			out.write(b);
	}
	
	public double getCrossoverFrequency() {
		return crossoverFrequency;
	}
	
	public void setCrossoverFrequency(double crossoverFrequency) {
		this.crossoverFrequency = crossoverFrequency;
	}
}
