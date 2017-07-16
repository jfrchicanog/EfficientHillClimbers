package neo.landscape.theory.apps.pseudoboolean.problems.factories;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public interface CommandLineFactory {
    public EmbeddedLandscape parseCommandLine(String [] args);

}
