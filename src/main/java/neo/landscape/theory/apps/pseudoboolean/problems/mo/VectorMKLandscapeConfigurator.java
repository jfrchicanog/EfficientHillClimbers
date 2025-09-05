package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.util.Properties;

public interface VectorMKLandscapeConfigurator {

    public void prepareOptionsForProblem(Options options);
    public VectorMKLandscape configureProblem(CommandLine commandLine, PrintStream ps);
    public VectorMKLandscape configureProblem(Properties properties, PrintStream ps);

}