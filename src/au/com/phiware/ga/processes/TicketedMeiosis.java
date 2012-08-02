package au.com.phiware.ga.processes;

import au.com.phiware.ga.Tickets;
import au.com.phiware.ga.containers.Ploid;

public class TicketedMeiosis<Individual extends Ploid<?>> extends Meiosis<Individual> {
	@Override
	public int getRepeatCount(Individual individual) {
		if (numberOfChromosomes <= 0)
			numberOfChromosomes(individual);
		return Tickets.getTickets(individual);
	}
}
