package au.com.phiware.ga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class Evolution<Individual extends Container> {
	private Collection<Individual> population;
	private Collection<Process> processes;
	private int generationCount = 0;

	public Evolution() {
		this(null, null);
	}
	
	public Evolution(Collection<Individual> population, Collection<Process> processes) {
		this.population = population;
		if (this.population == null)
			this.population = new HashSet<Individual>();
		
		this.processes = processes;
		if (this.processes == null)
			this.processes = new ArrayList<Process>();
	}

	/**
	 * @return the population
	 */
	public Collection<Individual> getPopulation() {
		return population;
	}

	/**
	 * @param <I>
	 * @param population the population to set
	 */
	public boolean addIndividual(Individual member) {
		return this.population.add(member);
	}
	
	/**
	 * @return the processes
	 */
	public Collection<Process> getProcesses() {
		return processes;
	}
	/**
	 * @param processes the processes to set
	 */
	public void addProcess(Process process) {
		this.processes.add(process);
	}
	
	public void evolve() throws EvolutionTransformException {
		for (Process process : processes)
			population = process.transform(population);
		++generationCount;
	}
	public void evolve(int generationCount) throws EvolutionTransformException {
		for (; generationCount > 0; generationCount--)
			evolve();
	}
}
