package neo.landscape.theory.apps.pseudoboolean.problems;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class MAXSATWriter {
	
	public static final String CNF_OPTION="cnf";
	
	private Options options;
	private CommandLine commandLine;
	private EmbeddedLandscapeConfigurator problemConfigurator;
	
	public MAXSATWriter() {
		problemConfigurator = new MAXSATConfigurator();
	}
	
	private Options getOptions() {
        if (options == null) {
            options = prepareOptions();
        }
        return options;
    }
	
	private Options prepareOptions() {
	    Options options = new Options();
	    problemConfigurator.prepareOptionsForProblem(options);
	    options.addOption(CNF_OPTION, "write output in CNF format");
	    return options;
	}
	
	private CommandLine parseCommandLine(String[] args) {
		try {
		    CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }
	
	protected void showOptions() {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp("maxsat", getOptions());
	}

	public void run(String[] args) {
		try {
			commandLine = parseCommandLine(args);
			PrintStream ps = System.err;
			
			MAXSAT problem = (MAXSAT) problemConfigurator.configureProblem(commandLine, ps);
			
			PrintWriter pw = new PrintWriter(System.out);
			if (commandLine.hasOption(CNF_OPTION)) {
				problem.writeInCNFFormat(pw);
			} else {
				problem.writeTo(pw);
			}
			pw.close();
			
		} catch (RuntimeException e) {
			System.err.println("Exception: "+e.getMessage());
			e.printStackTrace(System.err);
			showOptions();
		}

	}
	
	public static void main (String [] args) {
		new MAXSATWriter().run(args);
	}

}
