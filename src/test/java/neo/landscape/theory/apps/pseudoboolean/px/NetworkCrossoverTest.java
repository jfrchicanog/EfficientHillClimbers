package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.SumOfEmbeddedLandscapes;

public class NetworkCrossoverTest {
    private Random rnd;
    private PrintStream ps;
	private NKLandscapeConfigurator nkLandscapeConfigurator;
    
    @Before
    public void prepareData() {
    	try {
			ps = new PrintStream("/dev/null");
			nkLandscapeConfigurator = new NKLandscapeConfigurator();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
    
    @After
    public void closePrintStream() {
    	ps.close();
    }
    
    @Test
    public void test() {
    	IntStream.range(1, 10).map(n->100*n).forEach(n->
    		{IntStream.range(0, 10).forEach(
    			pseed->testSumOfAdjacenNKLandscapes(100, n, 3, 64, pseed));}
    		);
    }
       
    private void testSumOfAdjacenNKLandscapes(int maxSizeOfComponent, int n, int k, int q, long pseed) {
    	if (maxSizeOfComponent > n) {
    		throw new IllegalArgumentException ("maxsize is larger than n");
    	}
    	rnd = new Random (pseed);
    	
    	for (int i=0; i < 100; i++)
    		testOneInstance(maxSizeOfComponent, n, k, q); 
    }

	protected void testOneInstance(int maxSizeOfComponent, int n, int k, int q) {
		// Random partition of n
    	List<EmbeddedLandscape> partition = new ArrayList<>();
    	int acc = 0;
    	while (acc < n) {
    		int val = rnd.nextInt(n-acc)+1;
    		acc+=val;
    		
    		partition.add(nkLandscape(val, k, q));
    	}
    	EmbeddedLandscape el = new SumOfEmbeddedLandscapes(partition.toArray(new EmbeddedLandscape[0]));
    	
    	PBSolution zero = new PBSolution (el.getN());
    	PBSolution one = new PBSolution (zero);
    	for (int i = 0; i < one.getN(); i++) {
    		one.flipBit(i);
    	}
    	
    	NetworkCrossover nx = new NetworkCrossover(el);
    	nx.setSeed(rnd.nextLong());
    	nx.setMaximumSizeOfMask(maxSizeOfComponent);
    	nx.setPrintStream(ps);
    	
    	PBSolution child = nx.recombine(zero, one);
    	
    	int variableCount=0;
    	int countOfSemiFlippedComponents = 0;
    	int totalFromZeroParent=0;
    	for (EmbeddedLandscape landscape: partition) {
    		int fromZeroParent = 0;
    		for (int i=0; i < landscape.getN(); i++) {
    			if (child.getBit(i+variableCount) == zero.getBit(i+variableCount)) {
    				fromZeroParent++;
    			}
    		}
    		
    		totalFromZeroParent += fromZeroParent;
    		
    		if (fromZeroParent > 0 && fromZeroParent < landscape.getN()) {
    			countOfSemiFlippedComponents++;
    		}
    		
    		variableCount += landscape.getN();
    	}
    	
    	Assert.assertTrue("There are more than one semi flipped component", countOfSemiFlippedComponents<=1);
    	Assert.assertTrue("Not all bits flipped", totalFromZeroParent==maxSizeOfComponent || totalFromZeroParent==n-maxSizeOfComponent);
    	
	}

	protected EmbeddedLandscape nkLandscape(int val, int k, int q) {
		Properties prop = new Properties();
		prop.setProperty(NKLandscapeConfigurator.N_ARGUMENT, val+"");
		prop.setProperty(NKLandscapeConfigurator.K_ARGUMENT, k+"");
		prop.setProperty(NKLandscapeConfigurator.Q_ARGUMENT, q+"");
		prop.setProperty(NKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, rnd.nextLong()+"");
		prop.setProperty(NKLandscapeConfigurator.MODEL_ARGUMENT, "adjacent");
		EmbeddedLandscape configureProblem = nkLandscapeConfigurator.configureProblem(prop, ps);
		return configureProblem;
	}

}
