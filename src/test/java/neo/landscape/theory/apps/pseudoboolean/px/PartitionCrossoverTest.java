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

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PartitionCrossoverTest {
    
    private static final String OUTPUT_FILENAME = "testData.bin";
    private static final String PX_TEST_DATA_BIN = "/px/"+OUTPUT_FILENAME;
    private Map<Integer,Map<Long,List<PBSolution>>> solutions;
    private boolean recording;
    
    private Map<Long, List<PBSolution>> map;
    private List<PBSolution> list;

    @Before
    public void setUp() throws Exception {
        loadExpectedSolutions();
    }
    
    private void loadExpectedSolutions() {
        try {
            InputStream resource = getClass().getResourceAsStream(PX_TEST_DATA_BIN);
            ObjectInputStream objectInputStream = new ObjectInputStream(resource);
            solutions = (Map<Integer,Map<Long,List<PBSolution>>>)objectInputStream.readObject();
        } catch (Exception e) {
            recording=true;
            System.out.println("recording "+e.toString());
        }
    }

    private void writeSolutionIfNecessary() {
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
    public void testRecombine() {
        createSolutionsIfNecessary();
        for (int N : new int [] {100,500,1000}) {
            createMapIfNecessary();
            for (long seed : new long [] {0,50,100}) {
                
                NKLandscapes pbf = new NKLandscapes();
                Properties prop = new Properties();
                prop.setProperty(NKLandscapes.N_STRING, ""+N);
                prop.setProperty(NKLandscapes.K_STRING, "2");
                prop.setProperty(NKLandscapes.Q_STRING, "64");
                prop.setProperty(NKLandscapes.CIRCULAR_STRING, "no");

                pbf.setSeed(seed);
                pbf.setConfiguration(prop);
                
                PartitionCrossover px = new PartitionCrossover(pbf);
                px.setSeed(seed);
                
                createListIfNecessary();
                checkRecombination(pbf, px, N, seed);
                printSeedInsertionIfNecessary(seed);
                
            }
            printInsertionIfNecessary(N);
        }
        writeSolutionIfNecessary();
        
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

    private void printInsertionIfNecessary(int N) {
        if (recording) {
            solutions.put(N,map);
        }
    }

    private void printSeedInsertionIfNecessary(long seed) {
        if (recording) {
            map.put(seed,list);
        }
    }

    private void checkRecombination(EmbeddedLandscape el, PartitionCrossover px, int N, long seed) {
        List<PBSolution> listSolutions=null;
        if (!recording) {
            listSolutions = solutions.get(N).get(seed);
        }
        
        for (int i=0; i < 10; i++) {
            PBSolution solution = px.recombine(el.getRandomSolution(), el.getRandomSolution());
            printOrCheckSolutions((listSolutions==null)?null:listSolutions.get(i), solution);
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
