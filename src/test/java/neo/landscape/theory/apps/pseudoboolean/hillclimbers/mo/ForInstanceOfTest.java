package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

import java.util.Properties;

<<<<<<< HEAD
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;

import org.junit.Before;
=======
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
>>>>>>> 4a79125... Crossover refactored to adapt them to RBallHillClimber

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;

public class ForInstanceOfTest {
    private MultiObjectiveHammingBallHillClimberForInstanceOf forInstanceOf;

    @Before
    public void setUp() throws Exception {
        
    }

    protected MultiObjectiveHammingBallHillClimber buildHillClimber(int r) {
        Properties properties = new Properties();
        properties.setProperty(MultiObjectiveHammingBallHillClimber.R_STRING, ""+r);
        MultiObjectiveHammingBallHillClimber rball = new MultiObjectiveHammingBallHillClimber(properties);
        return rball;
    }
    
    private void buildHillClimber(int n, long seed, int r) {
        MultiObjectiveHammingBallHillClimber rball = buildHillClimber(r);
        VectorMKLandscape problem = buildNKLandscape(seed, n, 3, 100, false);
        forInstanceOf = (MultiObjectiveHammingBallHillClimberForInstanceOf)rball.initialize(problem);
    }
    
    protected VectorMKLandscape buildNKLandscape(long seed, int N, int K, int Q, boolean circular) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, ""+K);
        prop.setProperty(NKLandscapes.Q_STRING, ""+Q);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular?"yes":"no");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        

        return new VectorMKLandscape(pbf);
    }


}
