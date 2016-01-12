package neo.landscape.theory.apps.pseudoboolean.problems;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NKLandscapesTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testLocalicedMasks() {
        
        int K=5;
        int W=10;
        int N=200;
        
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, ""+K);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, ""+W);
        
        for (int seed =0; seed < 100; seed++) {
            testNKLandscapesConfiguration(W, prop, seed);
        }
    }
    
    @Test
    public void testAdjacentMasks() {
        
        int K=5;
        int N=100;
        
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, ""+K);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        
        for (int seed =0; seed < 100; seed++) {
            testNKLandscapesConfiguration(K, prop, seed);
        }
    }


    private void testNKLandscapesConfiguration(int window, Properties prop, long seed) {
        NKLandscapes nk = new NKLandscapes();
        nk.setSeed(seed);
        nk.setConfiguration(prop);
        
        int [][] masks = nk.getMasks();
        
        for (int subfunction=0; subfunction < masks.length; subfunction++) {
            Set<Integer> variables = new HashSet<Integer>();
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for (int variable: masks[subfunction]) {
                variables.add(variable);
                if (variable > max) {
                    max = variable;
                }
                if (variable < min) {
                    min = variable;
                }
            }
            Assert.assertEquals(masks[subfunction].length, variables.size());
            if (subfunction==min) {
                Assert.assertTrue(max <= min+window);
            }
            
        }
    }

}
