package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class DynasticPotentialCrossoverTest {
    private NKLandscapes nk;
    private DynasticPotentialCrossover pxap;
    private double alpha = 0.20;
    private Random rnd;

    @Test
    public void testAllSolutions() {
        
        IntStream.rangeClosed(1, 10).map(n->n*2).forEach(n->{
            int k=1;
            int q = 100;
            int pseed = 0;
            rnd = new Random(pseed);
            testAllSolutionsWithRandomNKLandscape(n, k, q, pseed);
        });
    }
    
    @Test
    public void test() {
        
        IntStream.range(1, 10).map(n->n*10).forEach(n->{
            int k=2;
            int q = 100;
            int pseed = 0;
            rnd = new Random(pseed);
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
        
        IntStream.rangeClosed(1,10).forEach(i->{
            PBSolution red = nk.getRandomSolution();
            PBSolution blue = new PBSolution(red);
            for (int flip=0; flip < (int)(alpha*n); flip++) {
            	blue.flipBit(rnd.nextInt(n));
            }
            testSolutionPair(red, blue);
        });
    }
    
    private void testAllSolutionsWithRandomNKLandscape(int n, int k, int q, int pseed) {
        nk = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+n);
        prop.setProperty(NKLandscapes.K_STRING, ""+k);
        prop.setProperty(NKLandscapes.Q_STRING, ""+q);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "random");
        nk.setConfiguration(prop);
        nk.setSeed(pseed);
        
        System.out.println("N:"+n);
        
        PBSolution red = new PBSolution(nk.getN());
        PBSolution blue = new PBSolution(red);
        for (int flip=0; flip < nk.getN(); flip++) {
        	blue.flipBit(flip);
        }
        testSolutionPair(red, blue);
    }
    
    private void testSolutionPair(PBSolution red, PBSolution blue) {
        pxap = new DynasticPotentialCrossover(nk);
        try {
            pxap.setDebug(true);
            pxap.setPrintStream(System.out);
            pxap.setPrintStream(new PrintStream(new FileOutputStream("/dev/null")));
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        PBSolution result = pxap.recombine(blue, red);
        
        Set<PBSolution> optimalChildren = computeAllOptimalChildren(blue, red);
        double expected = nk.evaluate(optimalChildren.iterator().next());
        double actualValue = nk.evaluate(result);
        
		Assert.assertEquals(expected, actualValue);
        Assert.assertTrue(optimalChildren.contains(result));

    }

	private Set<PBSolution> computeAllOptimalChildren(PBSolution blue, PBSolution red) {
		List<Integer> differingVariables = IntStream.range(0, nk.getN())
			.filter(i->blue.getBit(i)!=red.getBit(i))
			.boxed().collect(Collectors.toList());
		
		Set<PBSolution> result = new HashSet<>();
		double maxValue = Double.NEGATIVE_INFINITY;
		
		PBSolution trial = new PBSolution(red);
		for (int i=0; i < 1<<differingVariables.size(); i++) {
			int aux = i;
			for (int variable : differingVariables) {
				trial.setBit(variable, aux & 1);
				aux >>>= 1;
			}
			
			double value = nk.evaluate(trial);
			
			if (value > maxValue) {
				maxValue = value;
				result.clear();
				PBSolution toAdd = new PBSolution(trial);
				result.add(toAdd);
			} else if (value == maxValue) {
				PBSolution toAdd = new PBSolution(trial);
				result.add(toAdd);
			}
		}
		return result;
	}


}
