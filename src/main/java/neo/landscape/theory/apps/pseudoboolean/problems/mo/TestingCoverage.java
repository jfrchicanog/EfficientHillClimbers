package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

import java.io.Writer;
import java.util.Properties;

public class TestingCoverage extends EmbeddedLandscape {

     @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        return 0;
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        return 0;
    }

    @Override
    public void writeInstance(Writer writer) {
        // TODO
    }

    @Override
    public void setConfiguration(Properties prop) {
        // TODO
    }
}
