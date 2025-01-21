package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;
import java.util.Random;

public class FourierPartitionCrossoverConfigurator implements CrossoverConfigurator {

	private static final String DEBUG_ARGUMENT = "debug";
	private static final String RANDOMIZE_TIES = "randomizeTies";
	private static final String TARGET_MIN_COMPONENT_SIZE = "targetMinComponentSize";

	@Override
	public void prepareOptionsForCrossover(Options options) {
		options.addOption(DEBUG_ARGUMENT, false, "enable debug information (default: false)");
		options.addOption(RANDOMIZE_TIES, true, "randomize the decision in the components when there is a tie in the two parents (takes more time) (default=true)");
		options.addOption(TARGET_MIN_COMPONENT_SIZE, true, "minimum number of variables per component that is the target (not always possible) (defaul=1)");
	}

	@Override
	public CrossoverInternal configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		WalshCoefficientsInterface wcs = WalshTransform.transform(el, WalshCoefficientsArray.factory());
		WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
		FourierPartitionCrossover fpx = new FourierPartitionCrossover(wbf, el);

		boolean debug = properties.containsKey(DEBUG_ARGUMENT);
		fpx.setDebug(debug);
		fpx.setPrintStream(ps);

		if (properties.containsKey(RANDOMIZE_TIES)) {
			fpx.setRandomizeTies(Boolean.parseBoolean(properties.getProperty(RANDOMIZE_TIES)));
		}

		if (properties.containsKey(TARGET_MIN_COMPONENT_SIZE)) {
			fpx.setTargetMinComponentSize(Integer.parseInt(properties.getProperty(TARGET_MIN_COMPONENT_SIZE)));
		}

		return fpx;
	}

}
