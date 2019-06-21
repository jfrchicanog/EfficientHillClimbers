package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class DynasticPotentialCrossoverConfigurator implements CrossoverConfigurator {
	private static final String DEBUG_ARGUMENT = "debug";
	private static final String MAX_EXHAUSTIVE_EXPLORATION = "exhexp";

	@Override
	public void prepareOptionsForCrossover(Options options) {
		options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
        options.addOption(MAX_EXHAUSTIVE_EXPLORATION, true, "maximum number of variables to exhaustively explore in crossover (DPX): negative value is equivalent to no limit");
	}

	@Override
	public Crossover configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		DPXForRBallHillClimber px = new DPXForRBallHillClimber(el);
		boolean debug = properties.containsKey(DEBUG_ARGUMENT);
		int exhaustiveExploration = -1;
		if (properties.containsKey(MAX_EXHAUSTIVE_EXPLORATION)) {
			exhaustiveExploration = Integer.parseInt(properties.getProperty(MAX_EXHAUSTIVE_EXPLORATION));
		}
		
		px.setDebug(debug);
		px.setPrintStream(ps);
		if (exhaustiveExploration >= 0) {
			px.setMaximumVariablesToExhaustivelyExplore(exhaustiveExploration);
		}
		
		if (ps !=null) {
			ps.println("Exhexp: " + exhaustiveExploration);
		}
		
		return px;
	}

}
