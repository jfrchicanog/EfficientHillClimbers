package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveSelector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveSelector.KindOfMove;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.PseudoBooleanFunction;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.ConstrainedMNKLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.MNKLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.SingleThreadCPUTimer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ExactMultiObjectiveExperiment implements Process {

    private static final String PROBLEM_SEED_ARGUMENT = "pseed";
    private static final String MODEL_ARGUMENT = "model";
    private static final String Q_ARGUMENT = "q";
    private static final String K_ARGUMENT = "k";
    private static final String N_ARGUMENT = "n";
    private static final String D_ARGUMENT = "d";
    private static final String SHIFT_ARGUMENT = "shift";
    private static final String C_ARGUMENT = "c";
    
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private SingleThreadCPUTimer timer;

    private Options options;
    
    ParetoNonDominatedSet nonDominatedSet;
    
	@Override
	public String getDescription() {
		return "Multi-Objective (Constrained) Exhaustive Exploration for Constrained MNK Landscapes";
	}

    @Override
    public String getID() {
        return "mnk-exact";
    }

	@Override
	public String getInvocationInfo() {
	    HelpFormatter helpFormatter = new HelpFormatter();
	    StringWriter stringWriter = new StringWriter();
	    PrintWriter printWriter = new PrintWriter(stringWriter);

	    helpFormatter.printUsage(printWriter, Integer.MAX_VALUE, getID(), getOptions());
	    return stringWriter.toString();
	}

    private Options getOptions() {
        if (options == null) {
            options = prepareOptions();
        }
        return options;
    }
	
	private Options prepareOptions() {
	    Options options = new Options();
	    
	    options.addOption(N_ARGUMENT, true, "number of variables");
	    options.addOption(K_ARGUMENT, true, "number of subfunction arguments");
	    options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
	    options.addOption(MODEL_ARGUMENT, true, "NK-model: y->adjacent, n->random, <number>->Localized");
	    options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
	    options.addOption(D_ARGUMENT, true, "dimension of the problem");
	    options.addOption(C_ARGUMENT, true, "constraints of the problem");
	    options.addOption(SHIFT_ARGUMENT, true, "shift in the values for the tables");
	    
	    return options;
	}

	@Override
	public void execute(String[] args) {
	    
		if (args.length == 0) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(getID(), getOptions());
			return;
		}
		
		CommandLine commandLine = parseCommandLine(args);
		
		timer = new SingleThreadCPUTimer();
		timer.startTimer();
		
		initializeDataHolders();
		initializeOutput();
		
		ConstrainedMNKLandscape pbf = configureProblem(commandLine);
		
        ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());
        
        completeEnumeration(pbf);
        
        ps.println("Elapsed time: "+timer.elapsedTimeInMilliseconds());
        ps.println(nonDominatedSet.printArchive());

        printOutput();
    }

    protected void completeEnumeration(MNKLandscape pbf) {
        int n = pbf.getN();
        PBSolution sol = new PBSolution(n);
        int[] data = sol.getData();

        if (n >= 31) {
            throw new RuntimeException("A long search of " + n
                    + " bits. I will not do that!");
        }

        int limit = 1 << n;
        for (data[0] = 0; data[0] < limit; data[0]++) {
            double val [] = pbf.evaluate(sol);
            if (feasibleSolution(val, pbf.getConstraintIndex())) {
                nonDominatedSet.reportSolutionToArchive(val, pbf.getConstraintIndex());
            }
            
        }
    }

    private boolean feasibleSolution(double[] val, int constraintIndex) {
        boolean feasible = true;
        for (int i = constraintIndex; feasible && i < val.length; i++) {
            feasible &= (val[i] >= 0);
        }
        return feasible;
    }

    private ConstrainedMNKLandscape configureProblem(CommandLine commandLine) {
        String n = commandLine.getOptionValue(N_ARGUMENT);
        String k = commandLine.getOptionValue(K_ARGUMENT);
        String q = commandLine.getOptionValue(Q_ARGUMENT);
        String shift = commandLine.getOptionValue(SHIFT_ARGUMENT);
        int d = Integer.parseInt(commandLine.getOptionValue(D_ARGUMENT));
        int c = Integer.parseInt(commandLine.getOptionValue(C_ARGUMENT));
        String circular = commandLine.getOptionValue(MODEL_ARGUMENT);
        long problemSeed = Long.parseLong(commandLine.getOptionValue(PROBLEM_SEED_ARGUMENT));
        
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		prop.setProperty(MNKLandscape.DIMENSION_STRING, String.valueOf(d+c));
        prop.setProperty(ConstrainedMNKLandscape.CONSTRAINTS_STRING, String.valueOf(c));
        prop.setProperty(NKLandscapes.SHIFT_STRING, shift);

		if (!q.equals("-")) {
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}

		if (circular.equals("y")) {
			prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
		} else {
		    prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
		}
		
		ConstrainedMNKLandscape pbf = new ConstrainedMNKLandscape(problemSeed, prop);

		ps.println("N: " + n);
		ps.println("K: " + k);
		ps.println("Q: " + q);
		ps.println("D: " + d);
		ps.println("C: " + c);
        ps.println("Shift: " + shift);
		ps.println("NK-model: "+circular);
		ps.println("ProblemSeed: "+problemSeed);
        return pbf;
    }

    private CommandLine parseCommandLine(String[] args) {
		try {
		    CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }

    private void initializeDataHolders() {
        nonDominatedSet = new ParetoNonDominatedSet();
    }

    private void initializeOutput() {
        ba = new ByteArrayOutputStream();
        try {
            ps = new PrintStream(new GZIPOutputStream(ba));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printOutput() {
        ps.close();
        try {
            System.out.write(ba.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
