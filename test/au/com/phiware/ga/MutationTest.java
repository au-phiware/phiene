package au.com.phiware.ga;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.com.phiware.ga.Mutation.MutationOutputStream;

public class MutationTest {
	private class TestContainer implements Container {
		int data;

		@Override
		public void writeGenome(DataOutput out) throws IOException {
			out.writeInt(data);
		}

		@Override
		public void readGenome(DataInput in) throws IOException {
			data = in.readInt();
		}
	}

	private Mutation mutation;
	private TestContainer individual;
	@Before public void setUp() {
		Mutation.setMutationFrequency(1.0);
		mutation = new Mutation();
		individual = new TestContainer();
	}
	@After public void tearDown() {
		mutation = null;
		individual = null;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	@Test
	public void testGetGenomeFilters() throws IOException {
		OutputStream[] chain = Genomes.getGenomeFilters(individual, MutationOutputStream.class);
		assertSame("Should return 3 OutputStream.", 3, chain.length);
		ByteArrayOutputStream bytes = (ByteArrayOutputStream) chain[0];
		Mutation.MutationOutputStream mutator = (MutationOutputStream) chain[1];
		DataOutput data = (DataOutput) chain[2];
		assertNotNull(mutator);
		assertSame("Should mutate once for each byte.", 4, mutator.getMutationCount());
		byte[] genome = Genomes.getGenomeBytes(individual);
		assertArrayEquals("Should agree.", genome, bytes.toByteArray());
		for (byte b : genome)
			assertSame("Should set a single bit.", 1, Integer.bitCount(b & 0xFF));
		Genomes.setGenomeBytes(individual, genome);
		assertNotSame("Should mutate.", 0, individual.data);
	}

	@Test
	public void testTransformer() throws Exception {
		assertSame("Should return argument.", individual, mutation.transformer(individual).call());
		assertNotSame("Should mutate.", 0, individual.data);
	}

}
