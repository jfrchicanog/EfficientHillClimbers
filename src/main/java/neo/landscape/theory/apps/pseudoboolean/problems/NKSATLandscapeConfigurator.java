package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.experiments.EmbeddedLandscapeConfigurator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;
import java.util.stream.Stream;

public class NKSATLandscapeConfigurator implements EmbeddedLandscapeConfigurator {

    public static final String PROBLEM_SEED_ARGUMENT = "pseed";
    public static final String MODEL_ARGUMENT = "model";
    public static final String Q_ARGUMENT = "q";
    public static final String K_ARGUMENT = "k";
    public static final String N_ARGUMENT = "n";
    public static final String FACTOR = "alpha";
    public static final String STEP = "sat_step";
    public static final String INSTANCE_ARGUMENT = "sat_instance";
    public static final String FORCED_UNWEIGHTED_ARGUMENT = "sat_force_unweighted";
    public static final String MIN_ARGUMENT = "sat_min";
    public static final String HPINIT_ARGUMENT = "sat_hp";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(N_ARGUMENT, true, "number of variables");
        options.addOption(K_ARGUMENT, true, "number of subfunction arguments");
        options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
        options.addOption(MODEL_ARGUMENT, true, "NK-model: adjacent, random, <number>->Localized");
        options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
        options.addOption(FACTOR, true, "used to generate two nk problems, one with alpha times subfunctions and other (SAT) one minus alpha times");
        options.addOption(STEP, true, "used to generate SAT problem, this value is multiplied with alpha");options.addOption(INSTANCE_ARGUMENT, true, "file with the instance to load SAT problem");
        options.addOption(FORCED_UNWEIGHTED_ARGUMENT, false, "SAT: force unweighted instance, even if in wcnf format (optional)");
        options.addOption(MIN_ARGUMENT, false, "SAT: is minsat (optional)");
        options.addOption(HPINIT_ARGUMENT, false, "SAT: use hyperplane initialization (optional)");

    }

    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
        Properties properties = new Properties();

        Stream.of(N_ARGUMENT, K_ARGUMENT, Q_ARGUMENT, MODEL_ARGUMENT, PROBLEM_SEED_ARGUMENT, FACTOR, STEP,
                        INSTANCE_ARGUMENT, FORCED_UNWEIGHTED_ARGUMENT, MIN_ARGUMENT, HPINIT_ARGUMENT)
                .forEach(clave -> MAXSATConfigurator.moveProperty(commandLine, properties, clave));

        return configureProblem(properties, ps);
    }

    @Override
    public EmbeddedLandscape configureProblem(Properties properties, PrintStream ps) {
        Properties prop = new Properties();
        String n = properties.getProperty(NKSATLandscapeConfigurator.N_ARGUMENT);
        String k = properties.getProperty(NKSATLandscapeConfigurator.K_ARGUMENT);
        String q = properties.getProperty(NKSATLandscapeConfigurator.Q_ARGUMENT);
        String circular = properties.getProperty(NKSATLandscapeConfigurator.MODEL_ARGUMENT);
        String problemSeed = properties.getProperty(NKSATLandscapeConfigurator.PROBLEM_SEED_ARGUMENT);
        String factor = properties.getProperty(FACTOR);

        // NK arguments
        prop.setProperty(NKLandscapeConfigurator.N_ARGUMENT, n);
        prop.setProperty(NKLandscapeConfigurator.K_ARGUMENT, k);
        prop.setProperty(NKLandscapeConfigurator.Q_ARGUMENT, q);
        prop.setProperty(NKLandscapeConfigurator.MODEL_ARGUMENT, circular);
        prop.setProperty(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, problemSeed);
        prop.setProperty(NKLandscapeConfigurator.FACTOR, factor);
//        prop.setProperty(NKLandscapeConfigurator.IS_RANDOM, "");
        EmbeddedLandscape nkLandscape = new NKLandscapeConfigurator().configureProblem(prop, ps);

        // SAT arguments
        prop = new Properties();
//        if (properties.containsKey(INSTANCE_ARGUMENT)) {
//            String instance = properties.getProperty(INSTANCE_ARGUMENT);
//            prop.setProperty(MAXSATConfigurator.INSTANCE_ARGUMENT, instance);
//            if (properties.containsKey(FORCED_UNWEIGHTED_ARGUMENT)) {
//                prop.setProperty(MAXSATConfigurator.FORCED_UNWEIGHTED_ARGUMENT, "");
//            }
//        }else{
//            int K = Integer.parseInt(k) +1 ;
//            int maxK = 1 << K;
//            int m = (int) (Integer.parseInt(n) * 4.27);
//            double new_factor = 1 - Double.parseDouble(factor);
//
//            prop.setProperty(MAXSATConfigurator.N_ARGUMENT, n);
//            prop.setProperty(MAXSATConfigurator.MAX_K_ARGUMENT, String.valueOf(maxK));
//            prop.setProperty(MAXSATConfigurator.M_ARGUMENT, String.valueOf(m));
//            prop.setProperty(MAXSATConfigurator.PSEED, problemSeed);
//            prop.setProperty(MAXSATConfigurator.FACTOR, String.valueOf(new_factor));
//        }
//        if (properties.containsKey(MIN_ARGUMENT)) {
//            prop.setProperty(MAXSATConfigurator.MIN_ARGUMENT, "");
//        }
//        if (properties.containsKey(HPINIT_ARGUMENT)) {
//            prop.setProperty(MAXSATConfigurator.HPINIT_ARGUMENT, "");
//        }
//        EmbeddedLandscape maxSatProblem = new MAXSATConfigurator().configureProblem(prop, ps);

        // Generating SAT from Nk Landscape
        int m = (int) (Integer.parseInt(n) * 4.27);
        double new_factor = 1 - Double.parseDouble(factor);
        String step = properties.getProperty(STEP);
        prop.setProperty(NKLandscapeConfigurator.N_ARGUMENT, n);
        prop.setProperty(NKLandscapeConfigurator.K_ARGUMENT, k);
        prop.setProperty(NKLandscapeConfigurator.Q_ARGUMENT, q);
        prop.setProperty(NKLandscapeConfigurator.MODEL_ARGUMENT, circular);
        prop.setProperty(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, problemSeed);
        prop.setProperty(NKLandscapeConfigurator.FACTOR, String.valueOf(new_factor));
        prop.setProperty(NKLandscapeConfigurator.IS_SAT, "");
        prop.setProperty(NKLandscapeConfigurator.SAT_STEP, step);
        EmbeddedLandscape sat = new NKLandscapeConfigurator().configureProblem(prop, ps);


        return new SumOfEmbeddedLandscapes(nkLandscape, sat);
    }

}
