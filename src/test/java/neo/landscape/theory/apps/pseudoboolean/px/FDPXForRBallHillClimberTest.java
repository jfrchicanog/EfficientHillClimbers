package neo.landscape.theory.apps.pseudoboolean.px;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;
import java.util.Random;
import java.util.stream.IntStream;

@Ignore
public class FDPXForRBallHillClimberTest {
    
    private NKLandscapes nk;
    private RBallCrossoverAdaptor rballDPX;
    private FourierDynasticPotentialCrossover fdpx;
    private Random rnd;
    private double alpha = 0.15;

    @Test
    public void test() {
        IntStream.range(1, 10).map(n->n*1000).forEach(n->{
            int k=2;
            int q = 10;
            int pseed = 0;
            rnd = new Random (pseed);
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
            PBSolution blue = new PBSolution(red);
            for (int flip=0; flip < (int)(n*alpha); flip++) {
            	blue.flipBit(rnd.nextInt(n));
            }
            testSolutionPair(red, blue);
        });
    }
    
    private void testSolutionPair(PBSolution red, PBSolution blue) {
        WalshCoefficientsInterface wcs = WalshTransform.transform(nk, WalshCoefficientsArray.factory());
        WalshBasedFunction wbf = new WalshBasedFunction(nk.getN(), wcs);
        fdpx = new FourierDynasticPotentialCrossover(wbf, nk);
        rballDPX = new RBallCrossoverAdaptor(new DynasticPotentialCrossover(nk));
        
        PBSolution result = fdpx.recombine(blue, red);
        
        RBallEfficientHillClimberForInstanceOf rballfio = rballHillCLimber();

        RBallEfficientHillClimberSnapshot redRball = rballfio.initialize(red);
        RBallEfficientHillClimberSnapshot blueRball = rballfio.initialize(blue);

        RBallEfficientHillClimberSnapshot resultRball = rballDPX.recombine(blueRball, redRball);
        
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
