package au.com.phiware.ga.processes;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.phiware.ga.containers.Calculator;

public class CalculatorTournament extends
		TicketedTournament<Calculator<Byte>, Calculator<Byte>> {
	private final static Logger logger = LoggerFactory.getLogger(CalculatorTournament.class);
	private final Random random;
	
	public CalculatorTournament() {
		this(2);
	}

	protected CalculatorTournament(int numberOfParticipients) {
		super(numberOfParticipients);
		random = new Random();
	}

	@Override
	public List<Calculator<Byte>> compete(List<Calculator<Byte>> individuals)
			throws InterruptedException {
		Collections.sort(individuals, new Comparator<Calculator<Byte>>() {
			@Override public int compare(Calculator<Byte> c, Calculator<Byte> d) {
				//byte target = (byte) random.nextInt(0x100);
				byte target = (byte) 3;
				try {
					int dresult = d.calculate(target);
					int cresult = c.calculate(target);
					logger.info(String.format("%4d: %8X vs. %-8X ... %4d : %-4d\n", target, d.hashCode(), c.hashCode(), dresult, cresult));
					return dresult - cresult;
				} catch (IOException never) {
					return 0;
				}
			}
		});
		return individuals;
	}
	
	@Override
	public Calculator<Byte> resign(Calculator<Byte> individual) {
		return individual;
	}
}
