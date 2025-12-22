package neo.landscape.theory.apps.pseudoboolean.experiments.mo;

import neo.landscape.theory.apps.pseudoboolean.exactsolvers.MultiObjectiveAdjacentNKExactSolver;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.MNKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.util.IParetoNonDominatedSet;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet2D;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet2DEfficientFactory;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet2DFactory;
import neo.landscape.theory.apps.util.Process;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class MOAdjacentMNKDynProg implements Process {

    private static final String PROBLEM_CHAR = "P";
    private static final String PROBLEM="problem";
    private static final String MNK_PROBLEM = "mnk";
    private static final String OUTPUT_FILE_ARGUMENT = "output";

    private VectorMKLandscape pbf;

    private String outputFileName;
    private VectorMKLandscapeConfigurator configurator;
    private String problem;
    private Options options;
    private PrintStream ps;
    private ByteArrayOutputStream ba;

    private final Map<String, VectorMKLandscapeConfigurator> configurators = new HashMap<>();
    {
        configurators.put(MNK_PROBLEM, new MNKLandscapeConfigurator());
    }

    private void initializeOutput() {
        ba = new ByteArrayOutputStream();
        try {
            ps = new PrintStream(new GZIPOutputStream(ba));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "This experiment computes the Pareto front of Adjacent MNK Landscapes using dynamic programming";
    }

    @Override
    public String getID() {
        return "modp";
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

            pbf = getProblemConfigurator().configureProblem(
                commandLine.getOptionProperties(PROBLEM_CHAR), ps);

            MultiObjectiveAdjacentNKExactSolver<?> solver = new MultiObjectiveAdjacentNKExactSolver<>(new ParetoNonDominatedSet2DEfficientFactory());
            IParetoNonDominatedSet<?> paretoFront = solver.computeParetoFront(pbf);

            System.out.println("Pareto front size: " + paretoFront.size());
            if (outputFileName != null) {
                System.out.println("Writing in file " + outputFileName);
                writeParetoFrontInFile(paretoFront);
                System.out.println("written");
            }
        } catch (Exception e) {
            showOptions();
            System.err.println(e.getMessage());
        }
    }

    private void writeParetoFrontInFile(IParetoNonDominatedSet<?> paretoFront) {
    	try (FileOutputStream fos = new FileOutputStream(outputFileName);
    		 GZIPOutputStream gzos = new GZIPOutputStream(fos);
    		 PrintWriter writer = new PrintWriter(gzos)) 
    	{
            paretoFront.stream().forEach(point -> {
                writer.println(
                    Arrays.stream(point)
                            .mapToObj(Double::toString)
                                .collect(Collectors.joining("\t")));
            });
    	} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
