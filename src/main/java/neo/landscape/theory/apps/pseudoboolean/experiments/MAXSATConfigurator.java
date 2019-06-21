package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.PrintStream;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;

public class MAXSATConfigurator implements EmbeddedLandscapeConfigurator {

    public static final String N_ARGUMENT = "n";
    public static final String M_ARGUMENT = "m";
    public static final String MAX_K_ARGUMENT = "maxk";
    public static final String PSEED = "pseed";
    public static final String MIN_ARGUMENT = "min";
    public static final String HPINIT_ARGUMENT = "hp";
    public static final String INSTANCE_ARGUMENT = "instance";
    public static final String FORCED_UNWEIGHTED_ARGUMENT = "force_unweighted";

    @Override
    public void prepareOptionsForProblem(Options options) {
        options.addOption(PSEED, true, "problem seed (optional, required if no instance given)");
        options.addOption(N_ARGUMENT, true, "number of variables (optional, required if no instance given)");
        options.addOption(M_ARGUMENT, true, "number of clauses (optional, required if no instance given)");
        options.addOption(MAX_K_ARGUMENT, true, "max number of literals per clause (optional, required if no instance given)");
        options.addOption(MIN_ARGUMENT, false, "is minsat (optional)");
        options.addOption(HPINIT_ARGUMENT, false, "use hyperplane initialization (optional)");
        options.addOption(INSTANCE_ARGUMENT, true, "file with the instance to load (optional)");
        options.addOption(FORCED_UNWEIGHTED_ARGUMENT, false, "force unweighted instance, even if in wcnf format (optional)");
    }

    @Override
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps) {
    	Properties properties = new Properties();

    	Stream.of(N_ARGUMENT, M_ARGUMENT, MAX_K_ARGUMENT, PSEED, 
    			MIN_ARGUMENT, HPINIT_ARGUMENT, INSTANCE_ARGUMENT, 
    			FORCED_UNWEIGHTED_ARGUMENT)
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
		MAXSAT maxsat = new MAXSAT();
        Properties prop = new Properties();
        if (properties.containsKey(INSTANCE_ARGUMENT)) {
            String instance = properties.getProperty(INSTANCE_ARGUMENT);
            prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
            if (properties.containsKey(FORCED_UNWEIGHTED_ARGUMENT)) {
                prop.setProperty(MAXSAT.FORCE_UNWEIGHTED, "");
            }
            ps.println("Intance: "+instance);
        } else {
            long problemSeed = Long.parseLong(properties.getProperty(PSEED));
            String n = properties.getProperty(N_ARGUMENT);
            String m = properties.getProperty(M_ARGUMENT);
            String maxk = properties.getProperty(MAX_K_ARGUMENT);
            
            prop.setProperty(MAXSAT.N_STRING, n);
            prop.setProperty(MAXSAT.M_STRING, m);
            prop.setProperty(MAXSAT.MAX_K_STRING, maxk);
            
            ps.println("Problem seed: "+problemSeed);
            ps.println("N: "+n);
            ps.println("M: "+m);
            ps.println("MAX-k: "+maxk);
            
        }
        
        if (properties.containsKey(MIN_ARGUMENT)) {
            prop.setProperty(MAXSAT.MIN_STRING, "yes");
            ps.println("MINSAT: yes");
        } else {
            ps.println("MINSAT: no");
        }
        
        if (properties.containsKey(HPINIT_ARGUMENT)) {
            prop.setProperty(MAXSAT.HYPERPLANE_INIT, "yes");
            ps.println("HP init: yes");
        } else {
            ps.println("HP init: no");
        }
        
        maxsat.setConfiguration(prop);

        return maxsat;
	}

}
