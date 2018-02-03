package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.PrintStream;
import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

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
        MAXSAT maxsat = new MAXSAT();
        Properties prop = new Properties();
        if (commandLine.hasOption(INSTANCE_ARGUMENT)) {
            String instance = commandLine.getOptionValue(INSTANCE_ARGUMENT);
            prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
            if (commandLine.hasOption(FORCED_UNWEIGHTED_ARGUMENT)) {
                prop.setProperty(MAXSAT.FORCE_UNWEIGHTED, "");
            }
            ps.println("Intance: "+instance);
        } else {
            long problemSeed = Long.parseLong(commandLine.getOptionValue(PSEED));
            String n = commandLine.getOptionValue(N_ARGUMENT);
            String m = commandLine.getOptionValue(M_ARGUMENT);
            String maxk = commandLine.getOptionValue(MAX_K_ARGUMENT);
            
            prop.setProperty(MAXSAT.N_STRING, n);
            prop.setProperty(MAXSAT.M_STRING, m);
            prop.setProperty(MAXSAT.MAX_K_STRING, maxk);
            
            ps.println("Problem seed: "+problemSeed);
            ps.println("N: "+n);
            ps.println("M: "+m);
            ps.println("MAX-k: "+maxk);
            
        }
        
        if (commandLine.hasOption(MIN_ARGUMENT)) {
            prop.setProperty(MAXSAT.MIN_STRING, "yes");
            ps.println("MINSAT: yes");
        } else {
            ps.println("MINSAT: no");
        }
        
        if (commandLine.hasOption(HPINIT_ARGUMENT)) {
            prop.setProperty(MAXSAT.HYPERPLANE_INIT, "yes");
            ps.println("HP init: yes");
        } else {
            ps.println("HP init: no");
        }
        
        maxsat.setConfiguration(prop);

        return maxsat;
    }

}
