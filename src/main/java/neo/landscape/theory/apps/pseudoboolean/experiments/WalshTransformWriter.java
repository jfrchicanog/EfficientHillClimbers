package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.problems.*;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import neo.landscape.theory.apps.util.Process;
import org.apache.commons.cli.*;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class WalshTransformWriter implements Process {
    private static final String MAXSAT_PROBLEM = "maxsat";
    private static final String NK_PROBLEM = "nk";
    private static final String WALSH_PROBLEM = "walsh";
    private static final String PROBLEM_CHAR = "P";
    private static final String PROBLEM="problem";
    private static final String OUTPUT_FILE="output";

    private final Map<String, EmbeddedLandscapeConfigurator> configurators = new HashMap<>();
    {
        configurators.put(MAXSAT_PROBLEM, new MAXSATConfigurator());
        configurators.put(NK_PROBLEM, new NKLandscapeConfigurator());
        configurators.put(WALSH_PROBLEM, new WalshBasedFunctionConfigurator());
    }

    private Options options;
    private CommandLine commandLine;
    private EmbeddedLandscapeConfigurator problemConfigurator;
    private String problem;

    @Override
    public String getDescription() {
        return "Take a pseudo-Booelan problem and writes the Walsh transform to a file";
    }

    @Override
    public String getID() {
        return "walsh-writer";
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

    @Override
    public void execute(String[] args) {

        try {
            commandLine = parseCommandLine(args);
            problem = commandLine.getOptionValue(PROBLEM);
            String outputFile = commandLine.getOptionValue(OUTPUT_FILE);
            EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(
                commandLine.getOptionProperties(PROBLEM_CHAR), null);

            WalshCoefficientsInterface<?> wcs = WalshTransform.transform(pbf, WalshCoefficientsArray.factory());
            WalshBasedFunction<?> wbf = new WalshBasedFunction(pbf.getN(), wcs);

            try (FileOutputStream fout = new FileOutputStream(outputFile);
                 PrintWriter pw = new PrintWriter(fout)) {
                wbf.writeInstance(pw);
            }

        } catch (Exception e) {
            showOptions();
        }
    }

    private Options prepareOptions() {
        Options options = new Options();
        options.addOption(Option.builder(PROBLEM_CHAR)
            .numberOfArgs(2)
            .valueSeparator()
            .argName("property=value")
            .desc("properties for the problem")
            .build());

        options.addOption(Option.builder(OUTPUT_FILE)
            .hasArg()
            .argName("file")
            .desc("file to write the Walsh transform")
            .required()
            .build());

        return options;
    }

    protected void showOptions() {
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

    private EmbeddedLandscapeConfigurator getProblemConfigurator() {
        if (problemConfigurator==null) {
            problemConfigurator = createEmbeddedLandscapeConfigurator();
        }
        return problemConfigurator;
    }

    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        EmbeddedLandscapeConfigurator elc =  configurators.get(problem);
        if (elc == null) {
            throw new IllegalArgumentException("Problem "+problem+" is unknown");
        }
        return elc;
    }
}
