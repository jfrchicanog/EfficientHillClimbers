package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;

public class MNKLandscapeConfigurator implements VectorMKLandscapeConfigurator{
    private static final String PROBLEM_SEED_ARGUMENT = "pseed";
    private static final String MODEL_ARGUMENT = "model";
    private static final String Q_ARGUMENT = "q";
    private static final String K_ARGUMENT = "k";
    private static final String N_ARGUMENT = "n";
    private static final String D_ARGUMENT = "d";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(N_ARGUMENT, true, "number of variables");
        options.addOption(K_ARGUMENT, true, "number of subfunction arguments");
        options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
        options.addOption(D_ARGUMENT, true, "dimension of the problem");
        options.addOption(MODEL_ARGUMENT, true, "NK-model: y->adjacent, n->random, <number>->Localized");
        options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
    }

    @Override
    public VectorMKLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
        String n = commandLine.getOptionValue(N_ARGUMENT);
        String k = commandLine.getOptionValue(K_ARGUMENT);
        String q = commandLine.getOptionValue(Q_ARGUMENT);
        String d = commandLine.getOptionValue(D_ARGUMENT);
        String circular = commandLine.getOptionValue(MODEL_ARGUMENT);
        long problemSeed = Long.parseLong(commandLine.getOptionValue(PROBLEM_SEED_ARGUMENT));

        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);
        prop.setProperty(MNKLandscape.DIMENSION_STRING, d);
        prop.setProperty(PROBLEM_SEED_ARGUMENT, String.valueOf(problemSeed));
        prop.setProperty(NKLandscapes.Q_STRING, q);
        prop.setProperty(MODEL_ARGUMENT, circular);

        return configureProblem(prop, ps);
    }

    @Override
    public VectorMKLandscape configureProblem(Properties properties, PrintStream ps) {
        long problemSeed = Long.parseLong(properties.getProperty(PROBLEM_SEED_ARGUMENT));
        String n = properties.getProperty(NKLandscapes.N_STRING);
        String k = properties.getProperty(NKLandscapes.K_STRING);
        String q = properties.getProperty(NKLandscapes.Q_STRING);
        String d = properties.getProperty(MNKLandscape.DIMENSION_STRING);
        String circular = properties.getProperty(MODEL_ARGUMENT);
        if (q.equals("-")) {
            properties.remove(NKLandscapes.Q_STRING);
        }
        if (circular.equals("y")) {
            properties.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        } else {
            properties.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
        }

        MNKLandscape pbf = new MNKLandscape(problemSeed, properties);

        ps.println("N: " + n);
        ps.println("K: " + k);
        ps.println("Q: " + q);
        ps.println("D: " + d);
        ps.println("NK-model: "+circular);
        ps.println("ProblemSeed: "+problemSeed);
        return pbf;

    }
}
