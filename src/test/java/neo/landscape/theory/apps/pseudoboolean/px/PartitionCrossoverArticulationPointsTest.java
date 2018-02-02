package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverArticulationPoints.FlippedSolution;

import org.junit.Test;

public class PartitionCrossoverArticulationPointsTest {

    private NKLandscapes nk;
    private PartitionCrossoverArticulationPoints pxap;
    private PartitionCrossover px;

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
        px = new PartitionCrossover(nk);
        try {
            pxap.setPrintStream(System.out);
            pxap.setPrintStream(new PrintStream(new FileOutputStream("/dev/null")));
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        PBSolution result = pxap.recombine(blue, red);
        
        Map<Integer, FlippedSolution> apsToFlip = pxap.getAllArticulationPointsToFlip();
        double improvement = pxap.getOverallImprovement();
        
        checkImprovement(red, blue, result, apsToFlip, improvement);        
        checkBestImprovement(red, improvement);

    }

    protected void checkBestImprovement(PBSolution red, double improvement) {
        PBSolution result;
        double redValue = nk.evaluate(red);
        double overallImprovement = 0.0;
        for (Set<Integer> varsInComponent: pxap.getPartition()) {
            PBSolution trial = new PBSolution (red);
            for (int var: varsInComponent) {
                trial.flipBit(var);
            }
            double trialValue = nk.evaluate(trial);
            double improvementInComponent = Math.max(0.0,  trialValue-redValue);
            
            for (int ap: pxap.getAllArticulationPoints()) {
                if (varsInComponent.contains(ap)) {
                    trial.flipBit(ap);
                    result  = px.recombine(trial, red);
                    double resultValue = nk.evaluate(result);
                    double imp = Math.max(0.0, resultValue-redValue);
                    if (imp > improvementInComponent) {
                        improvementInComponent = imp;
                    }
                    trial.flipBit(ap);
                    
                    red.flipBit(ap);
                    result  = px.recombine(trial, red);
                    resultValue = nk.evaluate(result);
                    imp = Math.max(0.0, resultValue-redValue);
                    if (imp > improvementInComponent) {
                        improvementInComponent = imp;
                    }
                    red.flipBit(ap);
                }
            }
            overallImprovement += improvementInComponent;
        }
        Assert.assertEquals(overallImprovement, improvement);
    }

    protected void checkImprovement(PBSolution red, PBSolution blue, PBSolution result,
            Map<Integer, FlippedSolution> apsToFlip, double improvement) {
        double redValue = nk.evaluate(red);
        if (apsToFlip.isEmpty()) {
            double resultValue = nk.evaluate(result);
            Assert.assertEquals(resultValue-redValue, improvement);
        } else {
            apsToFlip.entrySet().stream().forEach(entry->{
                if (FlippedSolution.BLUE.equals(entry.getValue())) {
                    blue.flipBit(entry.getKey());
                } else {
                    red.flipBit(entry.getKey());
                }
            });
            
            
            result = px.recombine(blue, red);
            double resultValue = nk.evaluate(result);
            Assert.assertEquals(resultValue-redValue, improvement);
            
            apsToFlip.entrySet().stream().forEach(entry->{
                if (FlippedSolution.BLUE.equals(entry.getValue())) {
                    blue.flipBit(entry.getKey());
                } else {
                    red.flipBit(entry.getKey());
                }
            });
        }
    }
    
    

}
