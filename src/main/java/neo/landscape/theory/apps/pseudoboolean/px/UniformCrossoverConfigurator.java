package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class UniformCrossoverConfigurator implements CrossoverConfigurator {

	@Override
	public void prepareOptionsForCrossover(Options options) {
	}

	@Override
	public CrossoverInternal configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		return new UniformCrossover(el);
	}

}
