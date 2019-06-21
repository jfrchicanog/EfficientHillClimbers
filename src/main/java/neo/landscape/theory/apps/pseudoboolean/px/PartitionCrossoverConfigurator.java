package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class PartitionCrossoverConfigurator implements CrossoverConfigurator {
	private static final String DEBUG_ARGUMENT = "debug";

	@Override
	public void prepareOptionsForCrossover(Options options) {
		options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
	}

	@Override
	public Crossover configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		PartitionCrossoverForRBallHillClimber px = new PartitionCrossoverForRBallHillClimber(el);
		px.setPrintStream(ps);
		px.setDebug(properties.containsKey(DEBUG_ARGUMENT));
		return px;
	}

}
