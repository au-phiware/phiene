package au.com.phiware.ga;

import java.util.concurrent.ExecutorService;

import au.com.phiware.event.Emitter;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface Process<Ante extends Container, Post extends Container> extends Emitter {
	public void transformPopulation(
			final CloseableBlockingQueue<? extends Ante> in,
			final CloseableBlockingQueue<? super Post> out)
					throws TransformException;

	public ExecutorService takeSharedExecutor();

	public void shutdown();
}
