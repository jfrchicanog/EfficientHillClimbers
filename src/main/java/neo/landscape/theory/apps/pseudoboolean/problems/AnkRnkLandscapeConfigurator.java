package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.experiments.EmbeddedLandscapeConfigurator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;
import java.util.stream.Stream;

public class AnkRnkLandscapeConfigurator implements EmbeddedLandscapeConfigurator {
    public static final String PROBLEM_SEED_ARGUMENT = "pseed";
    public static final String Q_ARGUMENT = "q";
    public static final String K_ARGUMENT = "k";
    public static final String N_ARGUMENT = "n";
    public static final String FACTOR = "alpha";
    public static final String STEP = "sat_step";
    public static final String RANDOM_PBM = "random_pbm";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(N_ARGUMENT, true, "number of variables");
        options.addOption(K_ARGUMENT, true, "number of subfunction arguments");
        options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
        options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
        options.addOption(FACTOR, true, "used to generate two nk problems, one with alpha times subfunctions and other (SAT) one minus alpha times");
        options.addOption(STEP, true, "used to generate SAT problem, this value is multiplied with alpha");
        options.addOption(RANDOM_PBM, true, "either ANK or RNK, indicates which problem is random and other will be static when it comes to sub-functions");
    }

    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
        Properties properties = new Properties();

        Stream.of(N_ARGUMENT, K_ARGUMENT, Q_ARGUMENT, PROBLEM_SEED_ARGUMENT, FACTOR, STEP, RANDOM_PBM).forEach(clave -> MAXSATConfigurator.moveProperty(commandLine, properties, clave));

        return configureProblem(properties, ps);
    }

    @Override
    public EmbeddedLandscape configureProblem(Properties properties, PrintStream ps) {
        String n = properties.getProperty(N_ARGUMENT);
        String k = properties.getProperty(K_ARGUMENT);
        String q = properties.getProperty(Q_ARGUMENT);
        String problemSeed = properties.getProperty(PROBLEM_SEED_ARGUMENT);
        String factor = properties.getProperty(FACTOR);
        String random_pbm = properties.getProperty(RANDOM_PBM);

        // Generating Adjacent NK
        Properties prop = new Properties();
        prop.setProperty(NKLandscapeConfigurator.N_ARGUMENT, n);
        prop.setProperty(NKLandscapeConfigurator.K_ARGUMENT, k);
        prop.setProperty(NKLandscapeConfigurator.Q_ARGUMENT, q);
        prop.setProperty(NKLandscapeConfigurator.MODEL_ARGUMENT, "adjacent");
        prop.setProperty(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, problemSeed);
        if (prop.containsKey(FACTOR)) {
            prop.setProperty(NKLandscapeConfigurator.FACTOR, factor);
        }
        if ("ANK".equalsIgnoreCase(random_pbm)) {
            prop.setProperty(NKLandscapeConfigurator.IS_RANDOM, "");
        }
        EmbeddedLandscape nk = new NKLandscapeConfigurator().configureProblem(prop, ps);

        prop = new Properties();
        // Generating Random NK
        prop.setProperty(NKLandscapeConfigurator.N_ARGUMENT, n);
        prop.setProperty(NKLandscapeConfigurator.K_ARGUMENT, k);
        prop.setProperty(NKLandscapeConfigurator.Q_ARGUMENT, q);
        prop.setProperty(NKLandscapeConfigurator.MODEL_ARGUMENT, "random");
        prop.setProperty(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, problemSeed);
        if ("RNK".equalsIgnoreCase(random_pbm)) {
            prop.setProperty(NKLandscapeConfigurator.IS_RANDOM, "");
        }
        if (prop.containsKey(FACTOR)) {
            double new_factor = 1 - Double.parseDouble(factor);
            prop.setProperty(NKLandscapeConfigurator.FACTOR, String.valueOf(new_factor));
        }

        EmbeddedLandscape rnk = new NKLandscapeConfigurator().configureProblem(prop, ps);

        return new SumOfEmbeddedLandscapes(nk, rnk);
    }
}
