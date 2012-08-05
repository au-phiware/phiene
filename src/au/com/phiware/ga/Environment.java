package au.com.phiware.ga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import au.com.phiware.util.concurrent.ArrayCloseableBlockingQueue;
import au.com.phiware.util.concurrent.CloseableBlockingQueue;

public class Environment<Individual extends Container> {
	private Collection<Individual> population = new HashSet<Individual>();
	private List<Process<? extends Container, ? extends Container>> processes = new ArrayList<Process<? extends Container, ? extends Container>>();

	private int generationCount = 0;

	public Environment() {
		this(null, (Individual[]) null);
	}
	
	public Environment(Individual... population) {
		this(null, population);
	}

	public Environment(Process<? super Individual, ? extends Container> firstProcess) {
		this(firstProcess, (Individual[]) null);
	}
	
	public Environment(
			Process<? super Individual, ? extends Container> firstProcess,
			Individual... population) {
		if (population != null)
			Collections.addAll(this.population, population);
		
		if (firstProcess != null)
			processes.add(firstProcess);
	}

	/**
	 * @return the population
	 */
	public Collection<Individual> getPopulation() {
		return population;
	}

	/**
	 * @param <I>
	 * @param member to add to the population
	 */
	public boolean addToPopulation(Individual member) {
		return this.population.add(member);
	}

	/**
	 * Returns <tt>true</tt> if the population should be clear during this environments evolve cycle, <tt>false</tt> otherwise.
	 * Default implementation returns <tt>false</tt>.
	 * @return <tt>true</tt> if the population should be clear during this environments evolve cycle, <tt>false</tt> otherwise.
	 */
	public boolean shouldFlushPopulation() {
		return false;
	}

	/**
	 * @return the processes
	 */
	public List<Process<? extends Container, ? extends Container>> getProcesses() {
		return processes;
	}
	/**
	 * @param process to set as the first of this evolution.
	 */
	public void setFirstProcess(Process<? super Individual, ? extends Container> process) {
		if (!processes.isEmpty())
			throw new IllegalStateException("First process already set.");
		this.processes.add(process);
	}
	/**
	 * Adds the specified processes to the end of this evolution <em>in reverse order</em>.
	 * 
	 * @param reverseOrderedProcesses - processes to add to end of this evolution <em>in reverse order</em>.
	 * @param lastProcess to add to end of this evolution.
	 * @throws ClassCastException if the Post type of any process can not be cast to the Ante type of the following process.
	 */
	public void appendProcess(Process<? extends Container, ? extends Individual> lastProcess,
			Process<? extends Container, ? extends Container>...reverseOrderedProcesses) {
		if (processes.isEmpty())
			throw new IllegalStateException("Process list is empty, use setFirstProcess.");

		int priorSize = processes.size();
		
		try {
			Class<?> anteType, postType = AbstractProcess.actualPostType(processes.get(priorSize - 1));
			if (reverseOrderedProcesses != null)
			for(int i = reverseOrderedProcesses.length; i > 0;) {
				Process<? extends Container, ? extends Container> process = reverseOrderedProcesses[--i];
				
				anteType = AbstractProcess.actualAnteType(process);
				if (!anteType.isAssignableFrom(postType))
					throw new ClassCastException(postType.getName()+" cannot be cast to "+anteType.getName());
				postType = AbstractProcess.actualPostType(process);

				processes.add(process);
			}
			anteType = AbstractProcess.actualAnteType(lastProcess);
			if (!anteType.isAssignableFrom(postType))
				throw new ClassCastException(postType.getName()+" cannot be cast to "+anteType.getName());
			processes.add(lastProcess);
		} catch(RuntimeException e) {
			while (processes.size() > priorSize)
				processes.remove(priorSize);
			throw e;
		}
	}
	/**
	 * Adds the specified process to the end of this evolution.
	 * 
	 * @param process to add to end of this evolution.
	 */
	public void appendProcess(Process<? extends Container, ? extends Individual> process) {
		this.appendProcess(process, (Process<? extends Container, ? extends Container>[]) null);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void evolve() throws TransformException {
		if (population.isEmpty()) return;
		
		final Collection<Individual> pop = population;
		final CloseableBlockingQueue<? super Individual> feeder;
		final CloseableBlockingQueue<? extends Individual> eater;
		Future feedResult;
		CloseableBlockingQueue in;
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future> future = new ArrayList<Future>();
		int bufferSize = population.size() / 2 + 1;

		/* Begin feeding the queue that will become the input of the first process. */
		feeder = in = new ArrayCloseableBlockingQueue(bufferSize);
		feedResult = executor.submit(new Runnable() {
			public void run() {
				try {
					for (Individual i : pop)
						feeder.put(i);
				}
				catch (InterruptedException earlyExit) {}
			}
		});
		
		/* Chain the processes together and begin executing them. */
		for (final Process process : processes) {
			final CloseableBlockingQueue safeIn = in;
			final CloseableBlockingQueue safeOut = new ArrayCloseableBlockingQueue<Container>(bufferSize);
			future.add(executor.submit(new Runnable() {
				public void run() {
					try {
						process.transformPopulation(safeIn, safeOut);
					} finally {
						try {
							safeOut.close();
						}
						catch (InterruptedException earlyExit) {}
					}
				}
			}));
			in = safeOut;
		}
		eater = in;
		
		try {
			/* Gobble up the new population, after the feeder is fed. */
			feedResult.get();
			if (shouldFlushPopulation())
				population.clear();
			future.add(executor.submit(new Runnable() {
				public void run() {
					eater.drainTo(population);
					if (!Thread.interrupted()) {
						assert(eater.isClosed());
					}//else exit early
				}
			}));
			feeder.close();

			/* Block until all processes are finished, throw any exceptions encountered. */ 
			for (Future result : future)
				result.get();
		} catch (InterruptedException earlyExit) {
			executor.shutdown();
			try {
				// Use generous time out, since a second interrupt may kill it
				executor.awaitTermination(2, TimeUnit.MINUTES);
			} catch (InterruptedException impatient) {}
			if (!executor.isTerminated())
				executor.shutdownNow();
		} catch (ExecutionException e) {
			executor.shutdownNow();
			if (e.getCause() instanceof RuntimeException)
				throw (RuntimeException) e.getCause();
			else
				//TODO WTF
				throw new RuntimeException(e);
		}

		++generationCount;
	}
	
	public void evolve(int generationCount) throws TransformException {
		for (; generationCount > 0; generationCount--)
			evolve();
	}
}
