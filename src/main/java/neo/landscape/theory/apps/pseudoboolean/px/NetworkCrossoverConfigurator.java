package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class NetworkCrossoverConfigurator implements CrossoverConfigurator {
	protected static final String MAX_MASK_SIZE = "maxMaskSize";
	protected static final String MAX_MASK_SIZE_FRACTION ="maxMaskSizeFraction";
	protected static final double DEFAULT_FRACTION=0.5;

	@Override
	public void prepareOptionsForCrossover(Options options) {
        options.addOption(MAX_MASK_SIZE, true, "maximum number of variables to use in the mask (optional)");
        options.addOption(MAX_MASK_SIZE_FRACTION, true, "fraction of variables to use in the mask, unless "
        							+MAX_MASK_SIZE+" is set (optional, default="+DEFAULT_FRACTION+")");
	}

	@Override
	public CrossoverInternal configureCrossover(Properties properties, EmbeddedLandscape el, PrintStream ps) {
		NetworkCrossover nx =  new NetworkCrossover(el);
		double fraction = DEFAULT_FRACTION;
		if (properties.containsKey(MAX_MASK_SIZE_FRACTION)) {
			fraction = Double.parseDouble(properties.getProperty(MAX_MASK_SIZE_FRACTION));
		}
		int maxMaskSize = (int)(fraction*el.getN());
		if (properties.containsKey(MAX_MASK_SIZE)) {
			maxMaskSize = Integer.parseInt(properties.getProperty(MAX_MASK_SIZE));
		}
		if (ps!=null) {
			ps.println("NX maxMaskSize: "+maxMaskSize);
		}
		
		nx.setMaximumSizeOfMask(maxMaskSize);
		return nx;
	}

}
