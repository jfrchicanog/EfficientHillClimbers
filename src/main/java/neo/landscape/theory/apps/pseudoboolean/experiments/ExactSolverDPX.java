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
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.DPXBasedExactSolver;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSATConfigurator;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Timer;
import neo.landscape.theory.apps.util.Timers;

public class ExactSolverDPX implements Process {
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private Timer timer;
    private CommandLine commandLine;

	
	private Options options;
	private EmbeddedLandscapeConfigurator problemConfigurator;
	
    @Override
    public String getDescription() {
        return "Computes the exact solution of an Embedded Landscape using a hybrid between enumeration and Dynastic Potential Crossover";
    }

    @Override
    public String getID() {
        return "exact-dpx";
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
		
		timer = Timers.getDefaultTimer();
		timer.startTimer();
		
		initializeOutput();
		
		EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(commandLine, ps);
    	
    	DPXBasedExactSolver<EmbeddedLandscape> solver = new DPXBasedExactSolver<>();
    	SolutionQuality<EmbeddedLandscape> solutionQuality = solver.solveProblem(pbf);
    	
        ps.println("Optimal solution: " + solutionQuality.solution);
        ps.println("Optimal fitness: " + solutionQuality.quality);
        
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
