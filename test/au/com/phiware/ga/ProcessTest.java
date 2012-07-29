package au.com.phiware.ga;

import static org.junit.Assert.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.*;
import static org.hamcrest.core.Is.is;
import au.com.phiware.util.concurrent.ArrayCloseableBlockingQueue;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import au.com.phiware.util.concurrent.QueueClosedException;

public class ProcessTest {
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

	private ArrayCloseableBlockingQueue<TestContainer> in, out;
	private TestContainer individual;
	private ExecutorService executor;

	@Before public void setUp() {
		in = new ArrayCloseableBlockingQueue<TestContainer>(3);
		out = new ArrayCloseableBlockingQueue<TestContainer>(4);
		individual = new TestContainer();
		executor = Executors.newCachedThreadPool();
	}
	@After public void tearDown() {
		in = out = null;
		individual = null;
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testUnsupportedOperationException() throws InterruptedException {
		(new Process<TestContainer, TestContainer>(){
			@Override
			public <Individual extends TestContainer> Callable<TestContainer> transformer(CloseableBlockingQueue<Individual> in)
					throws InterruptedException {
				return null;
			}
			@Override
			public TestContainer transform(TestContainer individual) {
				return null;
			}
		}).transformPopulation(in, out);
	}

	@Test
	public void testOne() throws InterruptedException {
		in.put(individual);
		in.close();
		(new Process<TestContainer, TestContainer>(){
			@Override
			public TestContainer transform(TestContainer individual) {
				return individual;
			}
		}).transformPopulation(in, out);
		assertTrue("Queue, out, should be closed.", out.isClosed());
		assertEquals("Should pass through", individual, out.take());
	}

	@Test
	public void testFew() throws InterruptedException {
		in.put(new TestContainer());
		in.put(new TestContainer());
		in.put(new TestContainer());
		in.close();
		(new Process<TestContainer, TestContainer>(){
			@Override
			public TestContainer transform(TestContainer individual) {
				return individual;
			}
		}).transformPopulation(in, out);
		assertTrue("Queue, out, should be closed.", out.isClosed());
		assertThat(out.size(), is(3));
	}

	@Test
	public void testMany() throws InterruptedException, ExecutionException {
		final List<TestContainer> pop = new ArrayList<TestContainer>();
		Future<Boolean> result;
		executor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				try {
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.put(new TestContainer());
					in.close();
				} catch (QueueClosedException e) {
					return false;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return true;
			}});
		result = executor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				try {
					pop.add(out.take()); // 1
					pop.add(out.take()); // 2
					pop.add(out.take()); // 3
					pop.add(out.take()); // 4
					pop.add(out.take()); // 5
					pop.add(out.take()); // 6
					pop.add(out.take()); // 7
					pop.add(out.take()); // 8
					pop.add(out.take()); // 9
					pop.add(out.take()); // Oh-O...
				} catch (QueueClosedException e) {
					return true;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return false;
			}});
		(new Process<TestContainer, TestContainer>(){
			@Override
			public TestContainer transform(TestContainer individual) {
				return individual;
			}
		}).transformPopulation(in, out);
		assertTrue("Queue, out, should be closed.", out.isClosed());
		assertThat(result.get(), is(true));
		assertThat(pop.size(), is(9));
	}
}
