package au.com.phiware.ga.processes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import au.com.phiware.ga.AbstractProcess;
import au.com.phiware.ga.Container;
import au.com.phiware.ga.TransformException;
import au.com.phiware.util.concurrent.ArrayCloseableBlockingQueue;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import au.com.phiware.util.concurrent.QueueClosedException;

public abstract class SegregableProcess<Ante extends Container, Post extends Container> extends
		AbstractProcess<Ante, Post> {

	public abstract boolean shouldSegregate(Ante individual, CloseableBlockingQueue<Ante> queue)
			throws InterruptedException;

	@Override
	public void transformPopulation(
			final CloseableBlockingQueue<? extends Ante> in,
			final CloseableBlockingQueue<? super Post> out)
					throws TransformException {
		ExecutorService transformer = Executors.newCachedThreadPool();
		ExecutorService feeder = Executors.newCachedThreadPool();
		List<Future<?>> results = new LinkedList<Future<?>>();
		
		try {
			Collection<CloseableBlockingQueue<Ante>> categories = newCategories();
			try {
				for (;;) {
					final Ante individual = in.take();
					boolean segregated = false;
					for (Iterator<CloseableBlockingQueue<Ante>> i = categories.iterator();
							!segregated && i.hasNext();
					) {
						final CloseableBlockingQueue<Ante> cat = i.next();
						if (segregated = shouldSegregate(individual, cat)) {
							cat.preventClose();
							feeder.submit(new Runnable() {
								public void run() {
									try {
										cat.put(individual);
									} catch (RuntimeException e) {
										throw e;
									} catch (Exception e) {
										throw new TransformException(e);
									} finally {
										cat.permitClose();
									}
								}
							});
						}
					}
					if (!segregated) {
						final CloseableBlockingQueue<Ante> q = newCategory(individual);
						categories.add(q);
						results.add(transformer.submit(new Runnable() {
							public void run() {
								try {
									SegregableProcess.super.transformPopulation(q, out);
								} catch (RuntimeException e) {
									throw e;
								} catch (Exception e) {
									throw new TransformException(e);
								}
							}
						}));
					}
				}
			} catch (QueueClosedException expected) {}
			
			for (CloseableBlockingQueue<? extends Ante> q : categories)
				q.close();

			drainFutures(results);
		} catch (InterruptedException earlyExit) {
			@SuppressWarnings("unused")
			int dropCount = 0;
			
			transformer.shutdown();
			try {
				// Use generous time out, since a second interrupt may kill it
				transformer.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException impatient) {}
			
			if (!transformer.isTerminated())
				dropCount = transformer.shutdownNow().size();
			
			//dropCount += done count in results(?)
		}
	}

	protected CloseableBlockingQueue<Ante> newCategory(Ante individual) {
		if (individual == null)
			return new ArrayCloseableBlockingQueue<Ante>(0x10);
		return new ArrayCloseableBlockingQueue<Ante>(0x10, false, Collections.singleton(individual));
	}

	protected Collection<CloseableBlockingQueue<Ante>> newCategories() {
		return new HashSet<CloseableBlockingQueue<Ante>>();
	}
}
