package neo.landscape.theory.apps.pseudoboolean.problems;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.stream.Stream;

import neo.landscape.theory.apps.pseudoboolean.parsers.NKLandscapesDimacsLikeReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.experiments.EmbeddedLandscapeConfigurator;

public class NKLandscapeConfigurator implements EmbeddedLandscapeConfigurator {

    public static final String PROBLEM_SEED_ARGUMENT = "pseed";
    public static final String MODEL_ARGUMENT = "model";
    public static final String Q_ARGUMENT = "q";
    public static final String K_ARGUMENT = "k";
    public static final String N_ARGUMENT = "n";
	public static final String INSTANCE_ARGUMENT = "instance";
    
    @Override
    public  void prepareOptionsForProblem(Options options) {
        options.addOption(N_ARGUMENT, true, "number of variables");
        options.addOption(K_ARGUMENT, true, "number of subfunction arguments");
        options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
        options.addOption(MODEL_ARGUMENT, true, "NK-model: adjacent, random, <number>->Localized");
        options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
		options.addOption(INSTANCE_ARGUMENT, true, "file with the instance to load (optional)");
    }
    
    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
    	Properties properties = new Properties();

    	Stream.of(INSTANCE_ARGUMENT, N_ARGUMENT, K_ARGUMENT, Q_ARGUMENT, MODEL_ARGUMENT, PROBLEM_SEED_ARGUMENT)
    		.forEach(clave -> MAXSATConfigurator.moveProperty(commandLine, properties, clave));

        return configureProblem(properties, ps);
    }

	@Override
	public EmbeddedLandscape configureProblem(Properties properties, PrintStream ps) {
		NKLandscapes pbf;
		if (properties.containsKey(INSTANCE_ARGUMENT)) {
			String instance = properties.getProperty(INSTANCE_ARGUMENT);
			NKLandscapesDimacsLikeReader instanceReader = new NKLandscapesDimacsLikeReader();
			try (FileReader reader = new FileReader(instance)) {
				pbf = instanceReader.readInstance(reader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			ps.println("Intance: "+instance);
		} else {
			pbf = new NKLandscapes();
			Properties prop = new Properties();
			String n = properties.getProperty(NKLandscapeConfigurator.N_ARGUMENT);
			String k = properties.getProperty(NKLandscapeConfigurator.K_ARGUMENT);
			String q = properties.getProperty(NKLandscapeConfigurator.Q_ARGUMENT);
			String circular = properties.getProperty(NKLandscapeConfigurator.MODEL_ARGUMENT);
			long problemSeed = Long.parseLong(properties.getProperty(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT));


			prop.setProperty(NKLandscapes.N_STRING, n);
			prop.setProperty(NKLandscapes.K_STRING, k);

			if (!q.equals("-")) {
				prop.setProperty(NKLandscapes.Q_STRING, q);
			}

			if (circular.equals("y")) {
				prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
			} else {
				prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
			}

			pbf.setSeed(problemSeed);
			pbf.setConfiguration(prop);

			ps.println("N: " + pbf.getN());
			ps.println("K: " + pbf.getK());
			ps.println("Q: " + pbf.getQ());
			ps.println("Adjacent model?: "
				+ (NKLandscapes.NKModel.ADJACENT.equals(pbf.getNKModel()) ? "true" : "false"));
			ps.println("NK-model: " + circular);
			ps.println("ProblemSeed: " + problemSeed);
		}
        return pbf;
	}


}
