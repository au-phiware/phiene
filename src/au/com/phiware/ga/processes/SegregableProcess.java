package au.com.phiware.ga.processes;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import au.com.phiware.ga.AbstractProcess;
import au.com.phiware.ga.Container;
import au.com.phiware.ga.TransformException;
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

	public abstract CloseableBlockingQueue<Ante> segregateQueueFor(Ante individual)
			throws InterruptedException;

	public abstract Collection<CloseableBlockingQueue<Ante>> getQueues();

	public void closeSegregateQueues() throws InterruptedException {
		for (CloseableBlockingQueue<Ante> q : getQueues()) {
			q.close();
		}
	};

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
		Map<CloseableBlockingQueue<Ante>, Future<?>> qMap = new HashMap<CloseableBlockingQueue<Ante>, Future<?>>();
		ExecutorService transformer = newExecutor(0);
		
		try {
			try {
				ExecutorService feeder = takeExecutor();
				try {
					while (!in.isClosed() || !in.isEmpty()) {
						final Ante individual = in.take();
						final CloseableBlockingQueue<Ante> q = segregateQueueFor(individual);
						if (q != null) {
							if (!qMap.containsKey(q)) {
								qMap.put(q, transformer.submit(new SegregatedTransform(q, out)));
							}
							q.preventClose();
							feeder.submit(new Runnable() {
								public void run() {
									try {
										q.put(individual);
									} catch (Exception e) {
										throw new TransformException(e);
									} finally {
										q.permitClose();
									}
								}
							});
						}
					}
				} finally {
					giveExecutor(feeder);
				}
			} catch (QueueClosedException expected) {}
			
			closeSegregateQueues();

			drainFutures(qMap.values());
			
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

	public String getShortName() {
		return "Seg"+super.getShortName();
	}
}
