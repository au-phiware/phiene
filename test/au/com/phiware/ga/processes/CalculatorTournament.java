package au.com.phiware.ga.processes;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import au.com.phiware.ga.containers.Calculator;

public class CalculatorTournament extends
		TicketedTournament<Calculator<Byte>, Calculator<Byte>> {

	private final Random random;
	
	protected CalculatorTournament() {
		super(2);
		random = new Random();
	}

	@Override
	public List<Calculator<Byte>> compete(List<Calculator<Byte>> individuals)
			throws InterruptedException {
		Collections.sort(individuals, new Comparator<Calculator<Byte>>() {
			@Override public int compare(Calculator<Byte> c, Calculator<Byte> d) {
				byte target = (byte) random.nextInt(0x100);
				try {
					return d.calculate(target) - c.calculate(target);
				} catch (IOException never) {
					return 0;
				}
			}
		});
		return individuals;
	}
}
