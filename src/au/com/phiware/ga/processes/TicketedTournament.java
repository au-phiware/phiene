package au.com.phiware.ga.processes;

import static au.com.phiware.ga.Tickets.getTickets;
import static au.com.phiware.ga.Tickets.transferTickets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import au.com.phiware.ga.Container;
import au.com.phiware.ga.Environment;
import au.com.phiware.ga.EnvironmentalProcess;
import au.com.phiware.ga.Selection;
import au.com.phiware.ga.TransformException;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import cern.colt.map.PrimeFinder;

public abstract class TicketedTournament<Ante extends Container, Post extends Container>
		extends SegregableProcess<Ante, Post>
		implements Selection<Ante, Post>, EnvironmentalProcess<Post> {

	private transient Map<CloseableBlockingQueue<? extends Ante>, Integer> stakes = new WeakHashMap<CloseableBlockingQueue<? extends Ante>, Integer>();
	protected int numberOfParticipants;
	private Environment<Post> environment;
	
	protected TicketedTournament(int numberOfParticipants) {
		this.numberOfParticipants = numberOfParticipants;
	}
	
	public abstract List<Post> compete(List<Ante> individuals)
			throws InterruptedException;

	public int getStakes(CloseableBlockingQueue<? extends Ante> queue) {
		return 0; //TODO
	}
	
	@Override
	protected Collection<CloseableBlockingQueue<Ante>> newCategories() {
		TreeSet<CloseableBlockingQueue<Ante>> rv = new TreeSet<CloseableBlockingQueue<Ante>>(
				new Comparator<CloseableBlockingQueue<Ante>>() {
					@Override public int compare(CloseableBlockingQueue<Ante> q, CloseableBlockingQueue<Ante> p) {
					return _getStakes(p) - _getStakes(q);
				}
			}
		);
		for (int i = 1; i < 4; i = PrimeFinder.nextPrime(i + 1)) {
			CloseableBlockingQueue<Ante> q = newCategory(null);
			stakes.put(q, i);
			rv.add(q);
		}
		return rv;
	}

	@Override
	public boolean shouldSegregate(Ante individual,
			CloseableBlockingQueue<Ante> queue) throws InterruptedException {
		return getTickets(individual) >= _getStakes(queue);
	}
	
	private int _getStakes(CloseableBlockingQueue<? extends Ante> queue) {
		Integer rv = stakes.get(queue); 
		if (rv == null)
			stakes.put(queue, rv = getStakes(queue));
		return rv;
	}

	@Override public final Callable<Post> transformer(CloseableBlockingQueue<? extends Ante> in)
			throws InterruptedException {
		return null;
	}
	
	protected List<Future<Post>> submitTransformers(final CloseableBlockingQueue<? extends Ante> in, final ExecutorService executor) throws InterruptedException {
		List<Future<Post>> results = new LinkedList<Future<Post>>();
		final int stakes = _getStakes(in);
		for (;;) {
			final List<Ante> individuals = new ArrayList<Ante>(numberOfParticipants);
			if (in.drainTo(individuals, numberOfParticipants) != numberOfParticipants)
				break;
			
			final Future<List<Post>> transformer = executor.submit(new Callable<List<Post>>() {
				public List<Post> call() {
					try {
						List<Post> winners = compete(individuals);
						Iterator<Post> i = winners.iterator();
						if (i.hasNext()) {
							Post winnerTakesAll = i.next();
							while (i.hasNext())
								transferTickets(stakes, i.next(), winnerTakesAll);
						}
						return winners;
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new TransformException(e);
					}
				}
			});
			
			for (int i = 0; i < numberOfParticipants; i++) {
				final int index = i;
				results.add(new Future<Post>() {
					@Override public boolean cancel(boolean mayInterruptIfRunning) {
						return transformer.cancel(mayInterruptIfRunning);
					}
					@Override public Post get() throws InterruptedException, ExecutionException {
						List<Post> winners = transformer.get();
						if (index < winners.size())
							return winners.get(index);
						return null;
					}
					@Override public Post get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						List<Post> winners = transformer.get(timeout, unit);
						if (index < winners.size())
							return winners.get(index);
						return null;
					}
					@Override public boolean isCancelled() {
						return transformer.isCancelled();
					}
					@Override public boolean isDone() {
						return transformer.isDone();
					}
				});
			}
		}
		
		return results;
	}
	
	@Override public final Post transform(Ante individual) {
		return null;
	}
	
	@Override
	public void didAddToEnvironment(Environment<Post> e) {
		environment = e;
	}
}
