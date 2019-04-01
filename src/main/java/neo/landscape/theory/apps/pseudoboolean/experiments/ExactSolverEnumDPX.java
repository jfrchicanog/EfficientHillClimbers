package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.PBSolution.BitsOrder;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.DPXEnumBasedExactSolver;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.SingleThreadCPUTimer;

public class ExactSolverEnumDPX implements Process {
	
	private static final String ENUM_VARIABLES="enumVariables";
	private static final String FIXED_VARIABLES="fixedVariables";
	private static final String DEBUG="debug";
	
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private SingleThreadCPUTimer timer;
    private CommandLine commandLine;
    private int variablesToEnumerate;
    private PBSolution fixedVariables;
    private boolean debug;

	
	private Options options;
	private EmbeddedLandscapeConfigurator problemConfigurator;
	
    @Override
    public String getDescription() {
        return "Computes the exact solution of an Embedded Landscape using Dynastic Potential Crossover";
    }

    @Override
    public String getID() {
        return "exact-enum-dpx";
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
	    getProblemConfigurator().prepareOptionsForProblem(options);
	    options.addOption(ENUM_VARIABLES, true, "variables to enumerate outside DPX (optional, default=0)");
	    options.addOption(FIXED_VARIABLES, true, "binary string with values for variables to fix (optional, default=none)");
	    options.addOption(DEBUG, false, "debug");
	    return options;
	}
	
	private EmbeddedLandscapeConfigurator getProblemConfigurator() {
        if (problemConfigurator==null) {
            problemConfigurator = createEmbeddedLandscapeConfigurator();
        }
        return problemConfigurator;
    }
    
    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        return new MAXSATConfigurator();
    }
    
    
    @Override
    public void execute(String[] args) {
		if (args.length == 0) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(getID(), getOptions());
			return;
		}
		commandLine = parseCommandLine(args);
		
		timer = new SingleThreadCPUTimer();
		timer.startTimer();
		
		initializeOutput();
		
		EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(commandLine, ps);
		
		if (commandLine.hasOption(ENUM_VARIABLES)) {
			variablesToEnumerate = Integer.parseInt(commandLine.getOptionValue(ENUM_VARIABLES));
		}
		if (commandLine.hasOption(FIXED_VARIABLES)) {
			fixedVariables = PBSolution.toPBSolution(commandLine.getOptionValue(FIXED_VARIABLES), BitsOrder.LITTLE_ENDIAN);
		}
		ps.println("Variables enumerated: "+variablesToEnumerate);
		ps.println("Fixed variables: "+((fixedVariables==null)?"":fixedVariables.toString()));
		debug = commandLine.hasOption(DEBUG);
    	
    	DPXEnumBasedExactSolver<EmbeddedLandscape> solver = new DPXEnumBasedExactSolver<>(variablesToEnumerate, fixedVariables);
    	if (debug) {
    		solver.setDebug(debug);
    		solver.setPrintStream(ps);
    	}
    	SolutionQuality<EmbeddedLandscape> solutionQuality = solver.solveProblem(pbf);
    	
    	if (solutionQuality == null) {
    		ps.println("Incomplete exploration");
    	} else {
    		ps.println("Optimal solution: " + solutionQuality.solution);
    		ps.println("Optimal fitness: " + solutionQuality.quality);
    	}
        
        ps.println("Elapsed time (ms): " + timer.elapsedTimeInMilliseconds());
        
        printOutput();

    }

	
    
    private void printOutput() {
        ps.close();
        try {
            System.out.write(ba.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private CommandLine parseCommandLine(String[] args) {
		try {
		    CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }
    
    private void initializeOutput() {
        ba = new ByteArrayOutputStream();
        try {
            ps = new PrintStream(new GZIPOutputStream(ba));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
   
}
