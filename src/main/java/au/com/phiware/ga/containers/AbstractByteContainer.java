package au.com.phiware.ga.containers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import au.com.phiware.ga.ByteContainer;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public abstract class AbstractByteContainer implements ByteContainer {
	private byte[] genome;
	private int size;
	
	protected abstract byte[] initGenome() throws IOException;
	
	/* (non-Javadoc)
	 * @see au.com.phiware.ga.ByteContainer#getGenome()
	 */
	@Override
	public byte[] getGenome() throws IOException {
		if (genome == null || size == 0)
			setGenome(initGenome());

		return Arrays.copyOf(genome, genome.length);
	}

	/* (non-Javadoc)
	 * @see au.com.phiware.ga.ByteContainer#setGenome(byte[])
	 */
	@Override
	public void setGenome(byte[] genome) {
		if (genome == null)
			this.size = 0;
		else {
			if (this.genome == null || this.genome.length < genome.length)
				this.genome = new byte[genome.length];
			size = genome.length;
		    System.arraycopy(this.genome, 0, genome, 0, size);
		}
	}

	@Override
	public void writeGenome(DataOutput out) throws IOException {
		if (genome == null || size == 0)
			setGenome(initGenome());
		out.write(genome, 0, size);
	}

	@Override
	public void readGenome(DataInput in) throws IOException {
		if (genome != null && size > 0)
			in.readFully(genome, 0, size);
		else {
			genome = new byte[size];
			try {
				for (int i = 0; i < size; i++)
					genome[i] = in.readByte();
			} catch(EOFException ok) {}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		//FIXME: Environment.pipeLogger.info("die:{}", this);
		super.finalize();
	}
}
