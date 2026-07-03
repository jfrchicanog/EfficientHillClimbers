package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.experiments.EmbeddedLandscapeConfigurator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MultiObjLandscapeConfigurator implements EmbeddedLandscapeConfigurator {

    public static final String NUM_OBJECTIVES_ARG = "objs";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(NUM_OBJECTIVES_ARG, true, "Number of objectives (nObjectives)");
        // Instance files are passed dynamically via -P inst1=... -P inst2=... etc.
    }

    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
        Properties properties = new Properties();

        if (commandLine.hasOption(NUM_OBJECTIVES_ARG)) {
            properties.setProperty(NUM_OBJECTIVES_ARG, commandLine.getOptionValue(NUM_OBJECTIVES_ARG));
        }


        // We can't use Stream.of() for hardcoded keys anymore. We need to grab all dynamic instX arguments.
        Properties cmdProps = commandLine.getOptionProperties("P");
        if (cmdProps != null) {
            for (String key : cmdProps.stringPropertyNames()) {
                if (key.startsWith("inst") || key.equals(NUM_OBJECTIVES_ARG)) {
                    properties.setProperty(key, cmdProps.getProperty(key));
                }
            }
        }

        return configureProblem(properties, ps);
    }

    @Override
    public EmbeddedLandscape configureProblem(Properties properties, PrintStream ps) {

        if (!properties.containsKey(NUM_OBJECTIVES_ARG)) {
            throw new IllegalArgumentException("You must specify the number of objectives using 'objs' (e.g., -P objs=3)");
        }

        int nObjectives = Integer.parseInt(properties.getProperty(NUM_OBJECTIVES_ARG));
        List<EmbeddedLandscape> loadedLandscapes = new ArrayList<>();

        // Dynamically load inst1, inst2, ... instN
        for (int i = 1; i <= nObjectives; i++) {
            String instanceKey = "inst" + i;
            if (!properties.containsKey(instanceKey)) {
                throw new IllegalArgumentException("Missing instance file for objective " + i + ". Please provide -P " + instanceKey + "=filepath");
            }

            Properties prop = new Properties();
            prop.setProperty(NKLandscapeConfigurator.INSTANCE_ARGUMENT, properties.getProperty(instanceKey));
            EmbeddedLandscape land = new NKLandscapeConfigurator().configureProblem(prop, ps);
            loadedLandscapes.add(land);
        }

        return new MultiObjectiveLandscape(loadedLandscapes.toArray(new EmbeddedLandscape[0]));
    }
}