package neo.landscape.theory.apps.pseudoboolean.hillclimbers;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EfficientHillClimberTest {
    
    public static class Output {
        public int moves;
        public int [] histogram;
        public double quality;
    };
    
    private Map<Integer, Map<Long, Output>> randomN, randomR, adjacentN, adjacentR;
    private Output o;
    private Map<Long, Output> mapSeed;
    
    public EfficientHillClimberTest() {
        loadAdjacentRValues();
        loadAdjacentNValues();
        loadRandomRValues();
        loadRandomNValues();
    }


    private void loadAdjacentRValues() {
        adjacentR = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=4680;
        o.quality=703666.0;
        o.histogram= new int [] {0, 4680};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=4579;
        o.quality=699278.0;
        o.histogram= new int [] {0, 4579};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=4776;
        o.quality=701325.0;
        o.histogram= new int [] {0, 4776};
        mapSeed.put(20L,o);
        adjacentR.put(1,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=5729;
        o.quality=732370.0;
        o.histogram= new int [] {0, 4993, 736};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=5701;
        o.quality=727236.0;
        o.histogram= new int [] {0, 4969, 732};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=5932;
        o.quality=730997.0;
        o.histogram= new int [] {0, 5156, 776};
        mapSeed.put(20L,o);
        adjacentR.put(2,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6162;
        o.quality=740463.0;
        o.histogram= new int [] {0, 5119, 795, 248};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6065;
        o.quality=735681.0;
        o.histogram= new int [] {0, 5044, 769, 252};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6306;
        o.quality=738230.0;
        o.histogram= new int [] {0, 5251, 834, 221};
        mapSeed.put(20L,o);
        adjacentR.put(3,mapSeed);
        }

        private void loadAdjacentNValues() {
        adjacentN = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=560;
        o.quality=72949.0;
        o.histogram= new int [] {0, 492, 68};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=588;
        o.quality=73098.0;
        o.histogram= new int [] {0, 514, 74};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=565;
        o.quality=73411.0;
        o.histogram= new int [] {0, 497, 68};
        mapSeed.put(20L,o);
        adjacentN.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1159;
        o.quality=145690.0;
        o.histogram= new int [] {0, 1015, 144};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1191;
        o.quality=147069.0;
        o.histogram= new int [] {0, 1037, 154};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1114;
        o.quality=146234.0;
        o.histogram= new int [] {0, 988, 126};
        mapSeed.put(20L,o);
        adjacentN.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=2826;
        o.quality=365131.0;
        o.histogram= new int [] {0, 2460, 366};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=2881;
        o.quality=362351.0;
        o.histogram= new int [] {0, 2480, 401};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=2813;
        o.quality=366014.0;
        o.histogram= new int [] {0, 2417, 396};
        mapSeed.put(20L,o);
        adjacentN.put(5000,mapSeed);
        }

        private void loadRandomRValues() {
        randomR = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=5047;
        o.quality=702456.0;
        o.histogram= new int [] {0, 5047};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=5197;
        o.quality=702105.0;
        o.histogram= new int [] {0, 5197};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=5174;
        o.quality=702606.0;
        o.histogram= new int [] {0, 5174};
        mapSeed.put(20L,o);
        randomR.put(1,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6113;
        o.quality=725829.0;
        o.histogram= new int [] {0, 5595, 518};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6232;
        o.quality=726146.0;
        o.histogram= new int [] {0, 5684, 548};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6300;
        o.quality=726876.0;
        o.histogram= new int [] {0, 5745, 555};
        mapSeed.put(20L,o);
        randomR.put(2,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6620;
        o.quality=734192.0;
        o.histogram= new int [] {0, 5806, 631, 183};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6736;
        o.quality=733692.0;
        o.histogram= new int [] {0, 5912, 649, 175};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6726;
        o.quality=735082.0;
        o.histogram= new int [] {0, 5906, 650, 170};
        mapSeed.put(20L,o);
        randomR.put(3,mapSeed);
        }

        private void loadRandomNValues() {
        randomN = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=665;
        o.quality=73048.0;
        o.histogram= new int [] {0, 605, 60};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=585;
        o.quality=72865.0;
        o.histogram= new int [] {0, 537, 48};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=621;
        o.quality=73342.0;
        o.histogram= new int [] {0, 573, 48};
        mapSeed.put(20L,o);
        randomN.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1202;
        o.quality=144963.0;
        o.histogram= new int [] {0, 1097, 105};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1195;
        o.quality=145062.0;
        o.histogram= new int [] {0, 1079, 116};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1229;
        o.quality=144655.0;
        o.histogram= new int [] {0, 1141, 88};
        mapSeed.put(20L,o);
        randomN.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=3126;
        o.quality=363579.0;
        o.histogram= new int [] {0, 2837, 289};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=3038;
        o.quality=362901.0;
        o.histogram= new int [] {0, 2768, 270};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=3155;
        o.quality=364150.0;
        o.histogram= new int [] {0, 2877, 278};
        mapSeed.put(20L,o);
        randomN.put(5000,mapSeed);
        }

    
    @Test
    public void testAdjacentChangingR() {
        int N=10000;
        int K=2;
        int Q=100;
        boolean circular = true;
        String variable = "adjacentR";
        
        printPreambleIfNecessary(variable, adjacentR);
        for (int r: new int [] {1, 2, 3}) {
            creaMapSeedIfNecessary(adjacentR);
            for (long seed: new long [] {0, 10, 20} ) {
                createOutputIfNecessary(adjacentR);
                runAndCheckConfiguration(seed, r, N, K, Q, circular, (adjacentR==null)?null:adjacentR.get(r).get(seed));
                putInMapSeedIfNecessary(seed, adjacentR);
            }
            putInMapIfNecessary(variable, r, adjacentR);
        }
        printEpilogueIfNecessary(variable, adjacentR);

    }
    
    @Test
    public void testAdjacentChangingN() {
        int K=2;
        int Q=100;
        int r=2;
        boolean circular = true;
        String variable = "adjacentN";
        
        printPreambleIfNecessary(variable, adjacentN);
        for (int N: new int [] {1000, 2000, 5000}) {
            creaMapSeedIfNecessary(adjacentN);
            for (long seed: new long [] {0, 10, 20} ) {
                createOutputIfNecessary(adjacentN);
                runAndCheckConfiguration(seed, r, N, K, Q, circular, (adjacentN==null)?null:adjacentN.get(N).get(seed));
                putInMapSeedIfNecessary(seed, adjacentN);
            }
            putInMapIfNecessary(variable, N, adjacentN);
        }
        printEpilogueIfNecessary(variable, adjacentN);

    }

    private void putInMapIfNecessary(String variable, int N, Map<Integer, Map<Long, Output>> map) {
        if (map == null) {
            System.out.println(variable+".put("+N+",mapSeed);");
        }
    }

    private void putInMapSeedIfNecessary(long seed, Map<Integer, Map<Long, Output>> map) {
        if (map == null) {
            System.out.println("mapSeed.put("+seed+"L,o);");
        }
    }

    private void createOutputIfNecessary(Map<Integer, Map<Long, Output>> map) {
        if (map == null) {
            System.out.println("o = new Output();");
        }
    }

    private void creaMapSeedIfNecessary(Map<Integer, Map<Long, Output>> map) {
        if (map == null) {
            System.out.println("mapSeed = new HashMap<Long, Output>();");
        }
    }

    private void printPreambleIfNecessary(String variable, Map<Integer, Map<Long, Output>> map) {
        if (map == null) {
            String methodName = "load"+Character.toUpperCase(variable.charAt(0))+variable.substring(1)+"Values";
            ;
            System.out.println("private void "+methodName+"() {");
            System.out.println(variable+" = new HashMap<Integer, Map<Long, Output>>();");
        }
    }
    
    private void printEpilogueIfNecessary(String variable, Map<Integer, Map<Long, Output>> map) {
        if (map == null) {
            System.out.println("}\n");
        }
    }

    @Test
    public void testRandomChangingR() {
        int N=10000;
        int K=2;
        int Q=100;
        boolean circular = false;
        String variable = "randomR";
        
        printPreambleIfNecessary(variable, randomR);
        for (int r: new int [] {1, 2, 3}) {
            creaMapSeedIfNecessary(randomR);
            for (long seed: new long [] {0, 10, 20} ) {
                createOutputIfNecessary(randomR);
                runAndCheckConfiguration(seed, r, N, K, Q, circular, (randomR==null)?null:randomR.get(r).get(seed));
                putInMapSeedIfNecessary(seed, randomR);
            }
            putInMapIfNecessary(variable, r, randomR);
        }
        printEpilogueIfNecessary(variable, randomR);

    }
    
    @Test
    public void testRandomChangingN() {
        int K=2;
        int Q=100;
        int r=2;
        boolean circular = false;
        String variable = "randomN";
        
        printPreambleIfNecessary(variable, randomN);
        for (int N: new int [] {1000, 2000, 5000}) {
            creaMapSeedIfNecessary(randomN);
            for (long seed: new long [] {0, 10, 20} ) {
                createOutputIfNecessary(randomN);
                runAndCheckConfiguration(seed, r, N, K, Q, circular, (randomN==null)?null:randomN.get(N).get(seed));
                putInMapSeedIfNecessary(seed, randomN);
            }
            putInMapIfNecessary(variable, N, randomN);
        }
        printEpilogueIfNecessary(variable, randomN);
    }


    private void runAndCheckConfiguration(long seed, int r, int N, int K, int Q, boolean circular, Output out) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, ""+K);
        prop.setProperty(NKLandscapes.Q_STRING, ""+Q);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular?"yes":"no");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);

        RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r, seed);
        RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf) rball
                .initialize(pbf);

        PBSolution pbs = pbf.getRandomSolution();
        RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rballf
                .initialize(pbs);

        double imp;
        long moves = 0;

        rballs.resetMovesPerDistance();

        do {
            imp = rballs.move();
            moves++;
        } while (imp > 0);
        moves--;

        double final_quality = rballs.getSolutionQuality();

        if (out != null) {

            Assert.assertEquals("Discrepancy in moves (N="+N+", r="+r+", seed="+seed+")", out.moves,  moves);
            Assert.assertArrayEquals("Discrepancy in move histogram (N="+N+", r="+r+", seed="+seed+")", out.histogram, rballs.getMovesPerDinstance());
            Assert.assertEquals("Discrepancy in final quality (N="+N+", r="+r+", seed="+seed+")", out.quality,  final_quality, 0.0001);
        }
        else {
            System.out.println("o.moves="+moves+";");
            System.out.println("o.quality="+final_quality+";");
            String histogram = Arrays.toString(rballs.getMovesPerDinstance());
            histogram = histogram.replace('[', '{').replace(']', '}');
            System.out.println("o.histogram= new int [] "+histogram+";");
        }
//        System.out.println("N: "+N);
//        System.out.println("K: "+K);
//        System.out.println("Q: "+Q);
//        System.out.println("Seed: "+seed);
//        System.out.println("r: "+r);
//        System.out.println("Circular: "+circular);
//        System.out.println("Moves: " + moves);
//        System.out.println("Move histogram: " + Arrays.toString(rballs.getMovesPerDinstance()));
//        System.out.println("Final quality: " + final_quality);
    }

}
