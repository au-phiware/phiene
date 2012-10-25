package au.com.phiware.ga;

import au.com.phiware.util.concurrent.CloseableBlockingQueue;

public interface Process<Ante extends Container, Post extends Container> {
	public void transformPopulation(
			final CloseableBlockingQueue<? extends Ante> in,
			final CloseableBlockingQueue<? super Post> out)
					throws TransformException;

	public void shutdown();
}
