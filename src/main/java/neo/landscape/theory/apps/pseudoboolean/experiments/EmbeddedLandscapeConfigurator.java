package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public interface EmbeddedLandscapeConfigurator {

    public void prepareOptionsForProblem(Options options);
    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps);
    public EmbeddedLandscape configureProblem(Properties properties, PrintStream ps);

}