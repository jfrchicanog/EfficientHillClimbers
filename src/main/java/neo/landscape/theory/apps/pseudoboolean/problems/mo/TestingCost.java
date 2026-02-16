package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

import java.io.Writer;
import java.util.Properties;

public class TestingCost extends EmbeddedLandscape {
    private double [] costs;

    public TestingCost() {

    }

    @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        return costs[sf] * pbs.getBit(sf);
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        return costs[sf] * ((value >>> sf) & 1);
    }

    @Override
    public void writeInstance(Writer writer) {
        // TODO
    }

    @Override
    public void setConfiguration(Properties prop) {
        // TODO: read costs
        m = Integer.parseInt(prop.getProperty("tests"));
        n = m;
        masks = new int[m][];
        for (int i = 0; i < m; i++) {
            masks[i] = new int[] {i};
        }
    }
}
