package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;

public class FourierPartitionCrossoverConfigurator implements CrossoverConfigurator {

	@Override
	public void prepareOptionsForCrossover(Options options) {
	}

	@Override
	public CrossoverInternal configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		WalshCoefficients wcs = WalshTransform.transform(el);
		WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
		FourierPartitionCrossover fpx = new FourierPartitionCrossover(wbf);
		fpx.setPrintStream(ps);
		return fpx;
	}

}
