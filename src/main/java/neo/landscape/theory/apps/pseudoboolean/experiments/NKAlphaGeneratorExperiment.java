package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.problems.NKAlphaGenerator;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Timers;
import org.apache.commons.cli.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class NKAlphaGeneratorExperiment  implements Process {

    public static final String N_ARGUMENT = "n";
    public static final String K_ARGUMENT = "k";
    public static final String ALPHA_ARGUMENT = "alpha";
    public static final String SEED_ARGUMENT = "seed";
    public static final String OUTPUT_FILE_ARGUMENT = "outputFile";
    public static final String Q_ARGUMENT = "q";

    private Options options;

    @Override
    public String getDescription() {
        return "Generate NKAlpha landscapes with specific alpha values.";
    }

    @Override
    public String getID() {
        return "nka-gen";
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
        options.addOption(ALPHA_ARGUMENT, true, "alpha parameter for NKAlpha landscape");
        options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
        options.addOption(SEED_ARGUMENT, true, "random seed for generating the landscape");
        options.addOption(OUTPUT_FILE_ARGUMENT, true, "output file to save the generated landscape");
        return options;
    }

    @Override
    public String getInvocationInfo() {
        HelpFormatter helpFormatter = new HelpFormatter();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        helpFormatter.printUsage(printWriter, Integer.MAX_VALUE, getID(), getOptions());
        return stringWriter.toString();
    }

    private CommandLine parseCommandLine(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 5) {
            System.out.println("Insufficient arguments provided.");
            System.out.println(getInvocationInfo());
            return;
        }
        CommandLine commandLine = parseCommandLine(args);
        int N = Integer.parseInt(commandLine.getOptionValue(N_ARGUMENT));
        int K = Integer.parseInt(commandLine.getOptionValue(K_ARGUMENT));
        double alpha = Double.parseDouble(commandLine.getOptionValue(ALPHA_ARGUMENT));
        int Q = Integer.parseInt(commandLine.getOptionValue(Q_ARGUMENT));
        long seed = Long.parseLong(commandLine.getOptionValue(SEED_ARGUMENT));
        String outputFile = commandLine.getOptionValue(OUTPUT_FILE_ARGUMENT);

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             PrintWriter pw = new PrintWriter(fos)
        ) {

            NKAlphaGenerator.generateNKAlpha(N, K, Q, alpha, seed, pw);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }
}
