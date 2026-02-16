package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

import java.io.Writer;
import java.util.Properties;

public class MOTestSuiteMinimziation extends VectorMKLandscape {

    @Override
    public void setConfiguration(Properties prop) {

        configureEmbeddedLandscapes(new EmbeddedLandscape []{new TestingCost(), new TestingCoverage()});
    }
}
