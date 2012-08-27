package au.com.phiware.ga.io;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.com.phiware.ga.*;
import au.com.phiware.util.concurrent.ArrayCloseableBlockingQueue;

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

	private TestContainer individual;
	@Before public void setUp() {
		MutationOutputStream.defaultMutationFrequency = 1.0;
		individual = new TestContainer();
	}
	@After public void tearDown() {
		individual = null;
	}

	@Test
	public void testGetGenomeFilters() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		MutationOutputStream mutator = new MutationOutputStream(bytes);
		byte[] trans = Genomes.transformGenome(individual, bytes, mutator);
		byte[] genome = Genomes.getGenomeBytes(individual);
		assertArrayEquals("Should return 3 OutputStream.", genome, trans);
		assertSame("Should mutate once for each byte.", 4, mutator.getMutationCount());
		assertArrayEquals("Should agree.", genome, bytes.toByteArray());
		for (byte b : genome)
			assertSame("Should set a single bit.", 1, Integer.bitCount(b & 0xFF));
		Genomes.setGenomeBytes(individual, genome);
		assertNotSame("Should mutate.", 0, individual.data);
	}

	@Test
	public void testTransformer() throws Exception {
		ArrayCloseableBlockingQueue<TestContainer> in = new ArrayCloseableBlockingQueue<TestContainer>(1);
		in.add(individual);
		in.close();

		AbstractProcess<Container, Container> mutation = new AbstractProcess<Container, Container>(){
			@Override
			public Container transform(Container individual) {
				try {
					@SuppressWarnings("unchecked")
					OutputStream[] chain = Genomes.getGenomeFilters(individual, MutationOutputStream.class);
					ByteArrayOutputStream bytes = (ByteArrayOutputStream) chain[0];
					MutationOutputStream mutator = (MutationOutputStream) chain[1];
					
					if (mutator.getMutationCount() > 0)
						Genomes.setGenomeBytes(individual, bytes.toByteArray());
				} catch (Exception e) {
					throw new TransformException(e);
				}
				return individual;
			}
		};
		assertSame("Should return argument.", individual, mutation.transformer(in).call());
		assertNotSame("Should mutate.", 0, individual.data);
	}

}
