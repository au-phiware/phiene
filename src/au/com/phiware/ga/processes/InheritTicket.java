package au.com.phiware.ga.processes;

import static au.com.phiware.ga.containers.Ploids.getParents;
import static au.com.phiware.ga.Tickets.transferTickets;

import au.com.phiware.ga.AbstractProcess;
import au.com.phiware.ga.Transmission;
import au.com.phiware.ga.containers.Ploid;

/**
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public class InheritTicket<Parent extends Ploid<?>, Individual extends Ploid<Parent>>
		extends AbstractProcess<Individual, Individual>
		implements Transmission<Individual, Individual> {
	@Override
	public Individual transform(Individual individual) {
		for (Parent p : getParents(individual))
			transferTickets(1, p, individual);
		return individual;
	}
	
	public String getShortName() {
		return "Tick"+super.getShortName();
	}
}
