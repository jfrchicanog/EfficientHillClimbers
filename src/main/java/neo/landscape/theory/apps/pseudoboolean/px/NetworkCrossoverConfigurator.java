package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class NetworkCrossoverConfigurator implements CrossoverConfigurator {
	private static final String MAX_MASK_SIZE = "maxMaskSize";

	@Override
	public void prepareOptionsForCrossover(Options options) {
        options.addOption(MAX_MASK_SIZE, true, "maximum number of variables to use in the mask");
	}

	@Override
	public CrossoverInternal configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		NetworkCrossover nx =  new NetworkCrossover(el);
		String maxMaskSizeString = properties.getProperty(MAX_MASK_SIZE);
		nx.setMaximumSizeOfMask(Integer.parseInt(maxMaskSizeString));
		return nx;
	}

}
