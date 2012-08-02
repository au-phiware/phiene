package au.com.phiware.ga;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class Tickets {
	private static Map<Container, Integer> tickets = Collections.synchronizedMap(new WeakHashMap<Container, Integer>());

	public static void transferTickets(int amount, Container from, Container to) {
		if (tickets.containsKey(from)) {
			int had = tickets.get(from);
			amount = Math.min(amount, had);
			tickets.put(from, had - amount);
			if (tickets.containsKey(to))
				had = tickets.get(to);
			else
				had = 0;
			tickets.put(to, had + amount);
		}
	}
	
	public static int getTickets(Container individual) {
		if (tickets.containsKey(individual))
			return tickets.get(individual);
		return 0;
	}

	public static void setTickets(int amount, Container individual) {
		tickets.put(individual, amount);
	}
	
	public static int giveTickets(int amount, Container individual) {
		int had = 0;
		if (tickets.containsKey(individual))
			had  = tickets.get(individual);
		tickets.put(individual, had + amount);
		return had + amount;
	}
	
	public static int takeTickets(int amount, Container individual) {
		int had = 0;
		if (tickets.containsKey(individual))
			had  = tickets.get(individual);
		tickets.put(individual, had - amount);
		return had - amount;
	}
}
