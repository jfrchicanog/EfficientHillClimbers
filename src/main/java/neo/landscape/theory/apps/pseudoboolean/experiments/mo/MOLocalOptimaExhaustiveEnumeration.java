package neo.landscape.theory.apps.pseudoboolean.experiments.mo;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.*;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.util.Process;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

public class MOLocalOptimaExhaustiveEnumeration implements Process {

    private static final String PROBLEM_CHAR = "P";
    private static final String PROBLEM="problem";
    private static final String MNK_PROBLEM = "mnk";
    private static final String MQUBO = "mqubo";
    private static final String OUTPUT_FILE_ARGUMENT = "output";


    private static final long REPORT_PERIOD = 1L<<30;
    private VectorMKLandscape pbf;
    public List<PBSolution> localOptima;
    
    private String outputFileName;
    private VectorMKLandscapeConfigurator configurator;
    private String problem;
    private Options options;
    private PrintStream ps;
    private ByteArrayOutputStream ba;
    private ParetoNonDominatedSet paretoNonDominatedSet;

    private final Map<String, VectorMKLandscapeConfigurator> configurators = new HashMap<>();
    {
        configurators.put(MNK_PROBLEM, new MNKLandscapeConfigurator());
        configurators.put(MQUBO, new MquboConfigurator());
    }

    @Override
    public String getDescription() {
        return "This experiment computes all the Pareto Local Optima of multi-objective problems exploring the search  "
        + "space using an exhaustive algorithm";
    }

    @Override
    public String getID() {
        return "molo-exh";
    }

    private Options prepareOptions() {
        Options options = new Options();
        options.addOption(PROBLEM, true, "problem to be solved: "+configurators.keySet());
        options.addOption(Option.builder(PROBLEM_CHAR)
            .numberOfArgs(2)
            .valueSeparator()
            .argName("property=value")
            .desc("properties for the problem")
            .build());
        options.addOption(OUTPUT_FILE_ARGUMENT, true, "output file");

        return options;
    }

    private VectorMKLandscapeConfigurator getProblemConfigurator() {
        if (configurator==null) {
            configurator = createProblemConfigurator();
        }
        return configurator;
    }

    private VectorMKLandscapeConfigurator createProblemConfigurator() {
        VectorMKLandscapeConfigurator elc =  configurators.get(problem);
        if (elc == null) {
            throw new IllegalArgumentException("Problem "+problem+" is unknown");
        }
        return elc;
    }

    private void storeLocalOptima(PBSolution lo) {
        localOptima.add(new PBSolution(lo));
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

    private void showOptions() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(getID(), getOptions());

        try {
            Options problemOptions = new Options();
            getProblemConfigurator().prepareOptionsForProblem(problemOptions);
            helpFormatter.printHelp("Problem: "+problem, problemOptions);
        } catch (RuntimeException e) {
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

    public void execute(String[] args) {
        if (args.length == 0) {
            showOptions();
            return;
        }

        try {
            CommandLine commandLine = parseCommandLine(args);
            problem = commandLine.getOptionValue(PROBLEM);
            if (commandLine.hasOption(OUTPUT_FILE_ARGUMENT)) {
                outputFileName = commandLine.getOptionValue(OUTPUT_FILE_ARGUMENT);
            }

            localOptima = new ArrayList<>();
            paretoNonDominatedSet = new ParetoNonDominatedSet();
            pbf = getProblemConfigurator().configureProblem(
                commandLine.getOptionProperties(PROBLEM_CHAR), ps);

            completeEnumeration();

            System.out.println("Local optima: " + localOptima.size());

            if (outputFileName != null) {
                System.out.println("Writing in file " + outputFileName);
                writeLOInFile();
                System.out.println("written");
            }

        } catch (Exception e) {
            showOptions();
            System.err.println(e.getMessage());
        }
    }

    protected void completeEnumeration() {
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
            if (paretoLocalOptima(sol, val)) {
                storeLocalOptima(sol);
                paretoNonDominatedSet.reportSolutionToArchive(val, pbf.getConstraintIndex());
            }
        }

    }

    private boolean paretoLocalOptima(PBSolution sol, double[] val) {
        int n = sol.getN();
        PBSolution neighbor = new PBSolution(sol);
        for (int var=0; var < n; var++) {
            neighbor.flipBit(var);
            double neighborVal [] = pbf.evaluate(neighbor);
            neighbor.flipBit(var);
            if (paretoDominates(neighborVal, val)) {
                return false;
            }
        }
        return true;
    }

    private boolean paretoDominates(double[] neighborVal, double[] val) {
        boolean anyBetter = false;
        for (int i=0; i < val.length; i++) {
            if (neighborVal[i] < val[i]) {
                return false;
            } else if (neighborVal[i] > val[i]) {
                anyBetter = true;
            }
        }
        return anyBetter;
    }

    private void writeLOInFile() {
    	try (FileOutputStream fos = new FileOutputStream(outputFileName);
    		 GZIPOutputStream gzos = new GZIPOutputStream(fos);
    		 PrintWriter writer = new PrintWriter(gzos)) 
    	{
    		localOptima.forEach(solution->{
    			writer.println(solution.toHex()+" "+Arrays.toString(getPbf().evaluate(solution)));
    		});
            writer.print(paretoNonDominatedSet.printArchive());
    		
    		
    	} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	protected VectorMKLandscape getPbf() {
		return pbf;
	}

	public void setPbf(VectorMKLandscape pbf) {
		this.pbf = pbf;
	}

}
