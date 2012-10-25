package au.com.phiware.ga.processes;

import static au.com.phiware.ga.Tickets.getTickets;
import static au.com.phiware.ga.Tickets.transferTickets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

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
	public abstract Post resign(Ante individual);

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

	@Override
	public final Callable<Post> transformer(final CloseableBlockingQueue<? extends Ante> in)
			throws InterruptedException {
		final int stakes = _getStakes(in);
		final List<Ante> individuals = new ArrayList<Ante>(numberOfParticipants);
		
		if (drain(in, individuals, numberOfParticipants) < numberOfParticipants)
			return nullTransformer;

		return new Callable<Post>() {
			public Post call() {
				LoggerFactory.getLogger("au.com.phiware.ga.Process."+getShortName()).debug("transforming {}, {}, {}...", individuals.toArray());
				try {
					Post winnerTakesAll = null;
					List<Post> winners = compete(individuals);
					Iterator<Post> i = winners.iterator();
					Post loser;
					if (i.hasNext()) {
						winnerTakesAll = i.next();
						while (i.hasNext()) {
							transferTickets(stakes, loser = i.next(), winnerTakesAll);
							if (getTickets(loser) == 0) {
								i.remove();
								environment.removeFromPopulation(loser);
							}
						}
					}
					return winnerTakesAll;
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new TransformException(e);
				}
			}
		};
	}
	
	@Override public final Post transform(Ante individual) {
		return null;
	}
	
	@Override
	public void didAddToEnvironment(Environment<Post> e) {
		environment = e;
	}
	
	public String getShortName() {
		return "Tour"+super.getShortName();
	}
}
