package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.PrintStream;
import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes.NKModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class NKLandscapeConfigurator implements EmbeddedLandscapeConfigurator {

    public static final String PROBLEM_SEED_ARGUMENT = "pseed";
    public static final String MODEL_ARGUMENT = "model";
    public static final String Q_ARGUMENT = "q";
    public static final String K_ARGUMENT = "k";
    public static final String N_ARGUMENT = "n";
    
    @Override
    public  void prepareOptionsForProblem(Options options) {
        options.addOption(NKLandscapeConfigurator.N_ARGUMENT, true, "number of variables");
        options.addOption(NKLandscapeConfigurator.K_ARGUMENT, true, "number of subfunction arguments");
        options.addOption(NKLandscapeConfigurator.Q_ARGUMENT, true, "cardinality of subfunction domain");
        options.addOption(NKLandscapeConfigurator.MODEL_ARGUMENT, true, "NK-model: adjacent, random, <number>->Localized");
        options.addOption(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
    }
    
    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
        String n = commandLine.getOptionValue(NKLandscapeConfigurator.N_ARGUMENT);
        String k = commandLine.getOptionValue(NKLandscapeConfigurator.K_ARGUMENT);
        String q = commandLine.getOptionValue(NKLandscapeConfigurator.Q_ARGUMENT);
        String circular = commandLine.getOptionValue(NKLandscapeConfigurator.MODEL_ARGUMENT);
        long problemSeed = Long.parseLong(commandLine.getOptionValue(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT));
        
    	NKLandscapes pbf = new NKLandscapes();
    	Properties prop = new Properties();
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
    			+ (NKModel.ADJACENT.equals(pbf.getNKModel()) ? "true" : "false"));
    	ps.println("NK-model: "+circular);
    	ps.println("ProblemSeed: "+problemSeed);
        return pbf;
    }

}
