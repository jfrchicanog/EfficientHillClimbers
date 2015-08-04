package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import neo.landscape.theory.apps.pseudoboolean.Experiments;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PartitionCrossoverWithScoresExperimentTest {

    @Test
    public void testMain() {
        Experiments.main("px 1000 2 64 y 1 10 5 1".split(" "));
    }

}
