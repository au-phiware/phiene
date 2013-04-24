package au.com.phiware.ga.containers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import au.com.phiware.ga.ByteContainer;
import au.com.phiware.ga.Environment;

import cern.colt.list.ByteArrayList;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public abstract class AbstractByteContainer implements ByteContainer {
	private ByteArrayList genome;
	
	protected abstract byte[] initGenome() throws IOException;
	
	/* (non-Javadoc)
	 * @see au.com.phiware.ga.ByteContainer#getGenome()
	 */
	@Override
	public byte[] getGenome() throws IOException {
		if (genome == null)
			genome = new ByteArrayList(initGenome());
		
		return Arrays.copyOf(genome.elements(), genome.size());
	}

	/* (non-Javadoc)
	 * @see au.com.phiware.ga.ByteContainer#setGenome(byte[])
	 */
	@Override
	public void setGenome(byte[] genome) {
		if (genome == null)
			this.genome = null;
		else {
			if (this.genome == null)
				this.genome = new ByteArrayList(genome.clone());
			else
				this.genome.elements(genome.clone());
		}
	}

	@Override
	public void writeGenome(DataOutput out) throws IOException {
		if (genome == null)
			genome = new ByteArrayList(initGenome());
		out.write(genome.elements(), 0, genome.size());
	}

	@Override
	public void readGenome(DataInput in) throws IOException {
		if (genome != null)
			in.readFully(genome.elements(), 0, genome.size());
		else {
			genome = new ByteArrayList();
			try {
				for (;;)
					genome.add(in.readByte());
			} catch(EOFException ok) {}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		Environment.pipeLogger.info("die:{}", this);
		super.finalize();
	}
}
