package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;
import java.util.stream.Stream;

public class WalshBasedFunctionConfigurator implements EmbeddedLandscapeConfigurator {
    public static final String INSTANCE_ARGUMENT = "instance";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(INSTANCE_ARGUMENT, true, "file with the instance to load (optional)");
    }

    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
    	Properties properties = new Properties();

    	Stream.of(INSTANCE_ARGUMENT)
    		.forEach(clave -> moveProperty(commandLine, properties, clave));

        return configureProblem(properties, ps);
    }

    static public void moveProperty(CommandLine commandLine, Properties properties, String clave) {
		String value = commandLine.getOptionValue(clave);
    	if (value != null) {
    		properties.setProperty(clave, value);
    	}
	}

	@Override
	public EmbeddedLandscape configureProblem(Properties properties, PrintStream ps) {
		WalshBasedFunction walshFunction = new WalshBasedFunction(WalshCoefficientsArray.factory().create(1));
        Properties prop = new Properties();
        if (properties.containsKey(INSTANCE_ARGUMENT)) {
            String instance = properties.getProperty(INSTANCE_ARGUMENT);
            prop.setProperty(WalshBasedFunction.INSTANCE_STRING, instance);
            ps.println("Instance: "+instance);
        } else {
            throw new IllegalArgumentException("Instance file not found");
        }
        walshFunction.setConfiguration(prop);
        return walshFunction;
	}

}
