package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

//We need to fix this test, since it does not reflect the new wasy of expressing the NK Model
@Ignore
public class PartitionCrossoverForRBallTest {
    
    private static final String OUTPUT_FILENAME = "testDataRBall.bin";
    private static final String PX_TEST_DATA_BIN = "/px/"+OUTPUT_FILENAME;
    private boolean recording;
    
    private Map<Integer,Map<Long,List<PBSolution>>> solutions;
    private Map<Long, List<PBSolution>> map;
    private List<PBSolution> list;

    @Before
    public void setUp() throws Exception {
        loadExpectedSolutions(PX_TEST_DATA_BIN);
    }
    
    private void loadExpectedSolutions(String file) {
        try {
            InputStream resource = getClass().getResourceAsStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(resource);
            solutions = (Map<Integer,Map<Long,List<PBSolution>>>)objectInputStream.readObject();
        } catch (Exception e) {
            //recording=true;
            //System.out.println("recording "+e.toString());
        }
    }

    private void storeSolutionIfNecessary() {
        if (recording) {
            try {
                ObjectOutputStream fos = new ObjectOutputStream(new FileOutputStream(OUTPUT_FILENAME));
                fos.writeObject(solutions);
                fos.close();
            } catch (IOException e) {

            }
        }
    }
    
    @Test
    public void testRecombineRBall() {
        int r = 2;
        
        createSolutionsIfNecessary();
        for (int N : new int [] {100,500,1000}) {
            createMapIfNecessary();
            for (long seed : new long [] {0,50,100}) {
                
                NKLandscapes pbf = createNKLandscape(N, seed);
                RBallEfficientHillClimberForInstanceOf rballfio = createRBallFio(r, seed, pbf);
                
                PartitionCrossoverForRBallHillClimber px = new PartitionCrossoverForRBallHillClimber(pbf);
                px.setSeed(seed);
                
                createListIfNecessary();
                checkRecombinationRBall(pbf, rballfio, px, N, seed);
                addResultsSeedIfNecessary(seed);
                
            }
            addResulsIfNecessary(N);
        }
        storeSolutionIfNecessary();
        
    }

    private void checkRecombinationRBall(NKLandscapes pbf, RBallEfficientHillClimberForInstanceOf rballfio, 
            PartitionCrossoverForRBallHillClimber px, int n, long seed) {
        List<PBSolution> listSolutions=null;
        if (!recording) {
            listSolutions = solutions.get(n).get(seed);
        }
        
        for (int i=0; i < 10; i++) {
            PBSolution parent1 = pbf.getRandomSolution();
            PBSolution parent2 = pbf.getRandomSolution();
            
            RBallEfficientHillClimberSnapshot snapshot1 = rballfio.initialize(parent1);
            RBallEfficientHillClimberSnapshot snapshot2 = rballfio.initialize(parent2);

            RBallEfficientHillClimberSnapshot child = px.recombine(snapshot1, snapshot2);
            
            if (child != null) {
                child.checkConsistency();
            } 
            
            printOrCheckSolutions((listSolutions==null)?null:listSolutions.get(i), child==null?null:child.getSolution());
        }
        
    }

    private NKLandscapes createNKLandscape(int N, long seed) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, "2");
        prop.setProperty(NKLandscapes.Q_STRING, "64");
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "no");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }

    private RBallEfficientHillClimberForInstanceOf createRBallFio(int r, long seed, NKLandscapes pbf) {
        Properties rballProp = new Properties();
        rballProp.setProperty(RBallEfficientHillClimber.R_STRING, ""+r);
        rballProp.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
        rballProp.setProperty(RBallEfficientHillClimber.SEED, ""+seed);
        
        return (RBallEfficientHillClimberForInstanceOf)
                new RBallEfficientHillClimber(rballProp).initialize(pbf);
    }

    private void createListIfNecessary() {
        if (recording) {
            list = new ArrayList<PBSolution>();
        }
    }

    private void createMapIfNecessary() {
        if (recording) {
            map = new HashMap<Long, List<PBSolution>>();
        }
    }

    private void createSolutionsIfNecessary() {
        if (recording) {
            solutions = new HashMap<Integer, Map<Long,List<PBSolution>>>();
        }
    }

    private void addResulsIfNecessary(int N) {
        if (recording) {
            solutions.put(N,map);
        }
    }

    private void addResultsSeedIfNecessary(long seed) {
        if (recording) {
            map.put(seed,list);
        }
    }


    private void printOrCheckSolutions(PBSolution expected, PBSolution actual) {
        if (recording) {
            list.add(actual);
        } else {
            Assert.assertEquals(expected, actual);
        }
    }

}
