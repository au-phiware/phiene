package au.com.phiware.ga;

import static org.junit.Assert.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

	@Test
	public void test() throws IOException {
		Mutation.MutationOutputStream mutator = Genomes.getGenomeFilter(individual, Mutation.MutationOutputStream.class);
		assertNotNull(mutator);
		assertSame("Should mutate once for each byte.", 4, mutator.getMutationCount());
		byte[] genome = Genomes.getGenomeBytes(individual);
		assertArrayEquals("Should agree.", genome, mutator.toByteArray());
		for (byte b : genome)
			assertSame("Should set a single bit.", 1, Integer.bitCount(b & 0xFF));
		Genomes.setGenomeBytes(individual, genome);
		assertNotSame("Should mutate.", 0, individual.data);
	}

	@Test
	public void testTransform() throws IOException, EvolutionTransformException {
		mutation.transform(Collections.singleton(individual));
		assertNotSame("Should mutate.", 0, individual.data);
	}

}
