package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.PrintStream;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface EmbeddedLandscapeConfigurator {

    public void prepareOptionsForProblem(Options options);

    public EmbeddedLandscape configureProblem(CommandLine commandLine, PrintStream ps);

}