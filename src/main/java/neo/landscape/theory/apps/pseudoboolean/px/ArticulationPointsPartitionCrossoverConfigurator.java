package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class ArticulationPointsPartitionCrossoverConfigurator implements CrossoverConfigurator {
	private static final String DEBUG_ARGUMENT = "debug";
	private static final String NOAP_ARGUMENT = "noap";
    private static final String DEGREE_ARGUMENT = "degree";

	@Override
	public void prepareOptionsForCrossover(Options options) {
		options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
		options.addOption(NOAP_ARGUMENT, false, "disables the reflip of articulation points");
        options.addOption(DEGREE_ARGUMENT, false, "reports the degree of the articulation points");
	}

	@Override
	public Crossover configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		PXAPForRBallHillClimber px = new PXAPForRBallHillClimber(el);
        px.enableArticulationPointsAnalysis(!properties.containsKey(NOAP_ARGUMENT));
        px.setShowDegreeOfArticulationPoints(properties.containsKey(DEGREE_ARGUMENT));
        px.setDebug(properties.containsKey(DEBUG_ARGUMENT));

		return px;
	}

}
