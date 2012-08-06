package au.com.phiware.ga.processes;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import au.com.phiware.ga.AbstractProcess;
import au.com.phiware.ga.Container;
import au.com.phiware.ga.TransformException;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import au.com.phiware.util.concurrent.QueueClosedException;

public abstract class RepeatableProcess<Ante extends Container, Post extends Container> extends
		AbstractProcess<Ante, Post> {
	private class Repeatable implements Callable<Post> {
		private Ante individual;
		
		public Repeatable(Ante individual) {
			this.individual = individual;
		}
		
		public Post call() {
			try {
				return transform(individual);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new TransformException(e);
			}
		}
		
		public int repeatCount() {
			return getRepeatCount(individual);
		}
	}
	
	@Override
	public Callable<Post> transformer(CloseableBlockingQueue<? extends Ante> in)
			throws InterruptedException {
		return new Repeatable(in.take());
	}
	
	@Override
	protected List<Future<Post>> submitTransformers(final CloseableBlockingQueue<? extends Ante> in, final ExecutorService executor) throws InterruptedException {
		List<Future<Post>> results = new LinkedList<Future<Post>>();
		boolean repeat = isTransformRepeated();
		try {
			for (;;) {
				Callable<Post> transformer = transformer(in);
				
				if (transformer == null)
					throw new UnsupportedOperationException("Transformer expected");
				
				if (repeat && transformer instanceof RepeatableProcess.Repeatable)
					for (int i = ((Repeatable) transformer).repeatCount(); i > 0; i--)
						results.add(executor.submit(transformer));
				results.add(executor.submit(transformer));
			}
		} catch (QueueClosedException expected) {}
		return results;
	}
	
	/**
	 * Returns the number of times that this process' transform should be repeated for the specified individual.
	 * This implementation returns zero, meaning the transform will be executed once per individual.
	 * This method is only called when {@link #isTransformRepeated()} returns <tt>true</tt>.
	 * @param individual with which the this process' transform method will called.
	 * @return transform repeat count for specified individual.
	 */
	public int getRepeatCount(Ante individual) {
		return 0;
	}

	/**
	 * Returns <tt>true</tt> when this process produces more than one post-individual for each ante-individual, <tt>false</tt> otherwise.
	 * This implementation returns <tt>false</tt>.
	 * @return <tt>true</tt> when this process produces more than one post-individual for each ante-individual, <tt>false</tt> otherwise.
	 */
	public boolean isTransformRepeated() {
		return false;
	}

}
