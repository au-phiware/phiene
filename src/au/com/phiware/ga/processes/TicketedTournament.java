package au.com.phiware.ga.processes;

import static au.com.phiware.ga.Tickets.getTickets;
import static au.com.phiware.ga.Tickets.transferTickets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import au.com.phiware.ga.Container;
import au.com.phiware.ga.Environment;
import au.com.phiware.ga.EnvironmentalProcess;
import au.com.phiware.ga.Selection;
import au.com.phiware.ga.TransformException;
import au.com.phiware.util.concurrent.PausableArrayCloseableBlockingQueue;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;
import cern.colt.map.PrimeFinder;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public abstract class TicketedTournament<Ante extends Container, Post extends Container>
		extends SegregableProcess<Ante, Post>
		implements Selection<Ante, Post>, EnvironmentalProcess<Post> {

	private transient Map<CloseableBlockingQueue<Ante>, Integer> stakes = new WeakHashMap<CloseableBlockingQueue<Ante>, Integer>();
	private transient Map<Integer, CloseableBlockingQueue<Ante>> queues = new HashMap<Integer, CloseableBlockingQueue<Ante>>();
	protected int numberOfParticipants;
	private Environment<Post> environment;
	
	protected TicketedTournament(int numberOfParticipants) {
		this.numberOfParticipants = numberOfParticipants;
	}
	
	public abstract List<Post> compete(List<Ante> individuals)
			throws InterruptedException;
	public abstract Post resign(Ante individual);

	@Override
	public Collection<CloseableBlockingQueue<Ante>> getQueues() {
		return stakes.keySet();
	}

	@Override
	public void closeSegregateQueues() throws InterruptedException {
		super.closeSegregateQueues();
		stakes.keySet().retainAll(Collections.emptySet());
		queues.keySet().retainAll(Collections.emptySet());
	};

	@Override
	public CloseableBlockingQueue<Ante> segregateQueueFor(Ante individual) throws InterruptedException {
		int stakes = PrimeFinder.priorPrime(getTickets(individual));
		CloseableBlockingQueue<Ante> q = null;
		if (stakes > 0) {
			if (!queues.containsKey(stakes)) {
				q = new PausableArrayCloseableBlockingQueue<Ante>(0x10, environment.getContinue());
				queues.put(stakes, q);
				this.stakes.put(q, stakes);
			} else {
				q = queues.get(stakes);
			}
		}
		return q;
	}

	private int _getStakes(CloseableBlockingQueue<? extends Ante> queue) {
		Integer rv = stakes.get(queue); 

		if (rv == null)
			return 0;
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
