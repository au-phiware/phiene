package au.com.phiware.ga.processes;

import au.com.phiware.ga.Tickets;
import au.com.phiware.ga.Transmission;
import au.com.phiware.ga.containers.Ploid;

public class InheritTicket<Individual extends Ploid<?>> extends Transmission<Individual, Individual> {
	@Override
	public Individual transform(Individual individual) {
		for (Ploid<?> p : individual.getParents())
			Tickets.transferTickets(1, p, individual);
		return individual;
	}
}
