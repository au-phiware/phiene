package au.com.phiware.ga;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import au.com.phiware.ga.containers.*;
import au.com.phiware.ga.processes.*;

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
			new Fertilization<Haploid<Calculator<Byte>>, Calculator<Byte>>() {},
			new TicketedMeiosis<Calculator<Byte>>() {}
		);
		while (environment.getPopulation().size() < 1)
			environment.addToPopulation(Genomes.initGenome(Calculator.newCalculator(Byte.class)));
	}

}
