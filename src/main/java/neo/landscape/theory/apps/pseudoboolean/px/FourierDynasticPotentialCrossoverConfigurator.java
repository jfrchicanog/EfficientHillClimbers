package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;

public class FourierDynasticPotentialCrossoverConfigurator implements CrossoverConfigurator {
	private static final String DEBUG_ARGUMENT = "debug";
	private static final String MAX_EXHAUSTIVE_EXPLORATION = "exhexp";

	@Override
	public void prepareOptionsForCrossover(Options options) {
		options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
        options.addOption(MAX_EXHAUSTIVE_EXPLORATION, true, "maximum number of variables to exhaustively explore in crossover (DPX): negative value is equivalent to no limit");
	}

	@Override
	public CrossoverInternal configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		WalshCoefficientsInterface wcs = WalshTransform.transform(el, WalshCoefficientsArray.factory());
		WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
		FourierDynasticPotentialCrossover fdpx = new FourierDynasticPotentialCrossover(wbf, el);


		boolean debug = properties.containsKey(DEBUG_ARGUMENT);
		int exhaustiveExploration = -1;
		if (properties.containsKey(MAX_EXHAUSTIVE_EXPLORATION)) {
			exhaustiveExploration = Integer.parseInt(properties.getProperty(MAX_EXHAUSTIVE_EXPLORATION));
		}

		fdpx.setDebug(debug);
		fdpx.setPrintStream(ps);
		if (exhaustiveExploration >= 0) {
			fdpx.setMaximumVariablesToExhaustivelyExplore(exhaustiveExploration);
		}
		
		if (ps !=null) {
			ps.println("Exhexp: " + exhaustiveExploration);
		}
		
		return fdpx;
	}

}
