package au.com.phiware.ga;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import au.com.phiware.ga.containers.Calculator;
import au.com.phiware.ga.containers.Haploid;
import au.com.phiware.ga.processes.CalculatorTournament;
import au.com.phiware.ga.processes.Fertilization;
import au.com.phiware.ga.processes.InheritTicket;
import au.com.phiware.ga.processes.TicketedMeiosis;

public class CalculatorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private Environment<Calculator<Byte>> environment;

	@SuppressWarnings("unchecked")
	@Test
	public void test() throws IOException {
		environment = new Environment<Calculator<Byte>>(new CalculatorTournament());
		environment.appendProcess(
			new InheritTicket<Haploid<Calculator<Byte>>, Calculator<Byte>>() {},
			new Fertilization<Haploid<Calculator<Byte>>, Calculator<Byte>>() {
				@Override public Calculator<Byte> transform() {
					return Calculator.newCalculator(Byte.class);
				}
			},
			new TicketedMeiosis<Calculator<Byte>>() {}
		);
		while (environment.getPopulation().size() < 0x10)
			environment.addToPopulation(Genomes.initGenome(Calculator.newCalculator(Byte.class), new Random()));
		for (Calculator<Byte> individual : environment.getPopulation())
			Tickets.giveTickets(2, individual);
		environment.evolve(100);
		assertTrue(!environment.getPopulation().isEmpty());
	}

}
