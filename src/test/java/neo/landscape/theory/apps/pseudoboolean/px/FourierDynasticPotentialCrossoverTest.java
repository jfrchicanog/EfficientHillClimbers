package neo.landscape.theory.apps.pseudoboolean.px;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FourierDynasticPotentialCrossoverTest {
    private NKLandscapes nk;
    private FourierDynasticPotentialCrossover fdpx;
    private double alpha = 0.20;
    private int maxVariablesToExplore = 3;
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
    
    @Test
    public void testTruncatedSearch() {
        
        IntStream.rangeClosed(1, 5).map(n->n*20).forEach(n->{
            int k=4;
            int q = 100;
            int pseed = 0;
            rnd = new Random(pseed);
            testTruncatedSearchWithRandomNKLandscape(n, k, q, pseed);
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
    
    private void testTruncatedSearchWithRandomNKLandscape(int n, int k, int q, int pseed) {
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
            testTruncatedSearchSolutionPair(red, blue);
        });
    }
    
    private void testTruncatedSearchSolutionPair(PBSolution red, PBSolution blue) {
        WalshCoefficientsInterface wcs = WalshTransform.transform(nk, WalshCoefficientsArray.factory());
        WalshBasedFunction wbf = new WalshBasedFunction(nk.getN(), wcs);
        fdpx = new FourierDynasticPotentialCrossover(wbf, nk);
        fdpx.setMaximumVariablesToExhaustivelyExplore(maxVariablesToExplore);
        try {
            fdpx.setDebug(true);
            fdpx.setPrintStream(new PrintStream(new FileOutputStream("/dev/null")));
            fdpx.setPrintStream(System.out);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        PBSolution result = fdpx.recombine(blue, red);
        TwoStatesIntegerSet nonExploredVariables = fdpx.cliqueManagement.getNonExhaustivelyExploredVariables();
        
        List<Integer> exploredVariables = IntStream.range(0, nk.getN())
    			.filter(i->(blue.getBit(i)!=red.getBit(i)) && !nonExploredVariables.isExplored(i))
    			.boxed().collect(Collectors.toList());
        
        System.out.println("Truncated exploration: "+exploredVariables.size()+" variables, "+nonExploredVariables.getNumberOfExploredElements()+" non-explored variables");
        System.out.println("Groups of non exhaustively explored variables: "+ fdpx.cliqueManagement.getGroupsOfNonExhaustivelyExploredVariables());
        
        if (exploredVariables.size() > 29) {
        	throw new IllegalArgumentException("Too many explored variables, I cannot do this test");
        }
        
		double maxValue = Double.NEGATIVE_INFINITY;
		Set<PBSolution> optimalSolutions = new HashSet<>();
		
		for (PBSolution base : new PBSolution[] { red, blue }) {
			PBSolution trial = new PBSolution(base);
			for (int i = 0; i < 1 << exploredVariables.size(); i++) {
				int aux = i;
				for (int variable : exploredVariables) {
					trial.setBit(variable, aux & 1);
					aux >>>= 1;
				}

				double value = nk.evaluate(trial);

				if (value > maxValue) {
					maxValue = value;
					optimalSolutions.clear();
					optimalSolutions.add(new PBSolution(trial));
				} if (value == maxValue) {
					optimalSolutions.add(new PBSolution(trial));
				}
			}
		}
		
		Set<Integer> commonVariables = IntStream.range(0, nk.getN())
				.filter(var -> {
					int value = result.getBit(var);
					for (PBSolution sol: optimalSolutions) {
						if (sol.getBit(var) != value) {
							return false;
						}
					}
					return true;
				})
				.boxed()
				.collect(Collectors.toSet());
		
		System.out.print("Different variables:");
		for (int var=nk.getN()-1; var >=0;  var--) {
			if (!commonVariables.contains(var)) {
				System.out.print(var+",");
			}
		}
		System.out.println();
		
		System.out.println("Child:");
		for (int var=nk.getN()-1; var >=0;  var--) {
			if (!commonVariables.contains(var)) {
				System.out.print(result.getBit(var));
			}
		}
		System.out.println();

		System.out.println("Optimal solutions:");
		for (PBSolution sol : optimalSolutions) {
			for (int var = nk.getN() - 1; var >= 0; var--) {
				if (!commonVariables.contains(var)) {
					System.out.print(sol.getBit(var));
				}
			}
			System.out.println();
		}
		
		double childValue = nk.evaluate(result);
		Assert.assertTrue("Some child is not better than athe exhaustive sexploration",childValue >= maxValue);
    }
    
    private void testSolutionPair(PBSolution red, PBSolution blue) {
        WalshCoefficientsInterface wcs = WalshTransform.transform(nk, WalshCoefficientsArray.factory());
        WalshBasedFunction wbf = new WalshBasedFunction(nk.getN(), wcs);
        fdpx = new FourierDynasticPotentialCrossover(wbf, nk);
        try {
            fdpx.setDebug(true);
            fdpx.setPrintStream(System.out);
            fdpx.setPrintStream(new PrintStream(new FileOutputStream("/dev/null")));
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        PBSolution result = fdpx.recombine(blue, red);
        
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
		
		if (differingVariables.size() > 29) {
			throw new IllegalArgumentException("Too many differing variables, I cannot do this test");
		}
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
