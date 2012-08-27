package au.com.phiware.ga.processes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
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
	private class SegregatedTransform implements Runnable {
        private final CloseableBlockingQueue<? extends Ante> in;
		private final CloseableBlockingQueue<? super Post> out;

        public SegregatedTransform(CloseableBlockingQueue<? extends Ante> q, CloseableBlockingQueue<? super Post> p) {
			in = q;
			out = p;
		}
		public void run() {
            try {
                SegregableProcess.super.transformPopulation(in, out);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new TransformException(e);
            }
        }
	}
	
	public abstract boolean shouldSegregate(Ante individual, CloseableBlockingQueue<Ante> queue)
			throws InterruptedException;

	protected int drain(final CloseableBlockingQueue<? extends Ante> from, final List<Ante> to, final int size)
			throws InterruptedException, QueueClosedException {
		int drainCount = from.drainTo(to, size);

		if (Thread.interrupted()) // We need to get out of here
			throw new InterruptedException();

		if (drainCount < size)
			if (from.isClosed())
				throw new QueueClosedException();

		return drainCount;
	}

	@Override
	public void transformPopulation(
			final CloseableBlockingQueue<? extends Ante> in,
			final CloseableBlockingQueue<? super Post> out)
					throws TransformException {
		List<Future<?>> results = new LinkedList<Future<?>>();
		ExecutorService transformer = newExecutor(0);
		
		try {
			Collection<CloseableBlockingQueue<Ante>> categories = newCategories();
			for (final CloseableBlockingQueue<Ante> q : categories)
                results.add(transformer.submit(new SegregatedTransform(q, out)));
			try {
				ExecutorService feeder = takeExecutor();
				try {
					while (!in.isEmpty() || !in.isClosed()) {
						final Ante individual = in.take();
						boolean segregated = false;
						for (final CloseableBlockingQueue<Ante> cat : categories) {
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
								break;
							}
						}
						if (!segregated) {
							final CloseableBlockingQueue<Ante> q = newCategory(individual);
							categories.add(q);
							results.add(transformer.submit(new SegregatedTransform(q, out)));
						}
					}
				} finally {
					giveExecutor(feeder);
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
		} finally {
			transformer.shutdown();
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
	
	public String getShortName() {
		return "Seg"+super.getShortName();
	}
}
