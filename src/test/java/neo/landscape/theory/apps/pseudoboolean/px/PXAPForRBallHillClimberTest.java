package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.Properties;
import java.util.stream.IntStream;

import org.junit.Test;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class PXAPForRBallHillClimberTest {
    
    private NKLandscapes nk;
    private RBallCrossover rballPXAP;
    private PartitionCrossoverArticulationPoints pxap;

    @Test
    public void test() {
        
        IntStream.range(1, 10).map(n->n*1000).forEach(n->{
            int k=2;
            int q = 10;
            int pseed = 0;
            testWithRandomNKLandscape(n, k, q, pseed);
        });
    }
    
    private void testWithRandomNKLandscape(int n, int k, int q, int pseed) {
        nk = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+n);
        prop.setProperty(NKLandscapes.K_STRING, ""+k);
        prop.setProperty(NKLandscapes.Q_STRING, ""+q);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "random");
        nk.setConfiguration(prop);
        nk.setSeed(pseed);
        
        System.out.println("N:"+n);
        
        IntStream.range(1,10).forEach(i->{
            PBSolution red = nk.getRandomSolution();
            PBSolution blue = nk.getRandomSolution();
            testSolutionPair(red, blue);
        });
    }
    
    private void testSolutionPair(PBSolution red, PBSolution blue) {
        pxap = new PartitionCrossoverArticulationPoints(nk);
        rballPXAP = new RBallCrossoverAdaptor(new PartitionCrossoverArticulationPoints(nk));
        
        PBSolution result = pxap.recombine(blue, red);
        
        RBallEfficientHillClimberForInstanceOf rballfio = rballHillCLimber();

        RBallEfficientHillClimberSnapshot redRball = rballfio.initialize(red);
        RBallEfficientHillClimberSnapshot blueRball = rballfio.initialize(blue);

        RBallEfficientHillClimberSnapshot resultRball = rballPXAP.recombine(blueRball, redRball);
        
        Assert.assertEquals(nk.evaluate(result), resultRball.getSolutionQuality());
        Assert.assertEquals(nk.evaluate(result), nk.evaluate(resultRball.getSolution()));
        
        RBallEfficientHillClimberSnapshot resultRballFromScratch = rballfio.initialize(resultRball.getSolution());
        
        for(int sub=0; sub < nk.getM(); sub++) {
            Assert.assertEquals(resultRballFromScratch.getSubFunctionEvaluation(sub), 
                    resultRball.getSubFunctionEvaluation(sub));
        }
        
        for (int move = 0 ; move < resultRball.getNumberOfMoves(); move++) {
            Assert.assertEquals(resultRballFromScratch.getMoveImprovementByID(move), 
                    resultRball.getMoveImprovementByID(move));
        }
        
        
    }

    protected RBallEfficientHillClimberForInstanceOf rballHillCLimber() {
        Properties rballConfig = new Properties();
        rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
        rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, "1");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+nk.getSeed());

        RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) 
                new RBallEfficientHillClimber(rballConfig).initialize(nk);
        return rballfio;
    }

}
