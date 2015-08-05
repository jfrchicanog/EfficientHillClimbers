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


    private void loadRandomNValues() {
        randomN = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=649;
        o.quality=73183.0;
        o.histogram= new int [] {0, 590, 59};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=537;
        o.quality=72781.0;
        o.histogram= new int [] {0, 481, 56};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=588;
        o.quality=73229.0;
        o.histogram= new int [] {0, 544, 44};
        mapSeed.put(20L,o);
        randomN.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1152;
        o.quality=145641.0;
        o.histogram= new int [] {0, 1048, 104};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1110;
        o.quality=144739.0;
        o.histogram= new int [] {0, 996, 114};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1192;
        o.quality=145153.0;
        o.histogram= new int [] {0, 1072, 120};
        mapSeed.put(20L,o);
        randomN.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=2888;
        o.quality=363700.0;
        o.histogram= new int [] {0, 2606, 282};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=2827;
        o.quality=361468.0;
        o.histogram= new int [] {0, 2600, 227};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=2873;
        o.quality=364617.0;
        o.histogram= new int [] {0, 2635, 238};
        mapSeed.put(20L,o);
        randomN.put(5000,mapSeed);
    }


    private void loadRandomRValues() {
        randomR = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=4637;
        o.quality=703592.0;
        o.histogram= new int [] {0, 4637};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=4704;
        o.quality=702118.0;
        o.histogram= new int [] {0, 4704};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=4842;
        o.quality=706623.0;
        o.histogram= new int [] {0, 4842};
        mapSeed.put(20L,o);
        randomR.put(1,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=5609;
        o.quality=725546.0;
        o.histogram= new int [] {0, 5089, 520};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=5684;
        o.quality=724208.0;
        o.histogram= new int [] {0, 5165, 519};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=5783;
        o.quality=727537.0;
        o.histogram= new int [] {0, 5276, 507};
        mapSeed.put(20L,o);
        randomR.put(2,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6066;
        o.quality=734023.0;
        o.histogram= new int [] {0, 5271, 617, 178};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6183;
        o.quality=732755.0;
        o.histogram= new int [] {0, 5363, 646, 174};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6265;
        o.quality=734907.0;
        o.histogram= new int [] {0, 5469, 595, 201};
        mapSeed.put(20L,o);
        randomR.put(3,mapSeed);
    }


    private void loadAdjacentNValues() {
        adjacentN = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=582;
        o.quality=72904.0;
        o.histogram= new int [] {0, 511, 71};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=562;
        o.quality=73377.0;
        o.histogram= new int [] {0, 499, 63};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=569;
        o.quality=73309.0;
        o.histogram= new int [] {0, 500, 69};
        mapSeed.put(20L,o);
        adjacentN.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1139;
        o.quality=145611.0;
        o.histogram= new int [] {0, 997, 142};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1200;
        o.quality=147489.0;
        o.histogram= new int [] {0, 1056, 144};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1110;
        o.quality=146353.0;
        o.histogram= new int [] {0, 974, 136};
        mapSeed.put(20L,o);
        adjacentN.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=2806;
        o.quality=365571.0;
        o.histogram= new int [] {0, 2473, 333};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=2890;
        o.quality=363273.0;
        o.histogram= new int [] {0, 2502, 388};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=2781;
        o.quality=366490.0;
        o.histogram= new int [] {0, 2414, 367};
        mapSeed.put(20L,o);
        adjacentN.put(5000,mapSeed);
    }


    private void loadAdjacentRValues() {
        adjacentR = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=4691;
        o.quality=706563.0;
        o.histogram= new int [] {0, 4691};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=4640;
        o.quality=701974.0;
        o.histogram= new int [] {0, 4640};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=4833;
        o.quality=703620.0;
        o.histogram= new int [] {0, 4833};
        mapSeed.put(20L,o);
        adjacentR.put(1,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=5674;
        o.quality=733388.0;
        o.histogram= new int [] {0, 5003, 671};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=5666;
        o.quality=727642.0;
        o.histogram= new int [] {0, 4983, 683};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=5898;
        o.quality=730969.0;
        o.histogram= new int [] {0, 5124, 774};
        mapSeed.put(20L,o);
        adjacentR.put(2,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6086;
        o.quality=741117.0;
        o.histogram= new int [] {0, 5104, 741, 241};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6051;
        o.quality=735734.0;
        o.histogram= new int [] {0, 5072, 735, 244};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6243;
        o.quality=738325.0;
        o.histogram= new int [] {0, 5220, 803, 220};
        mapSeed.put(20L,o);
        adjacentR.put(3,mapSeed);
        
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
            System.out.println(variable+" = new HashMap<Integer, Map<Long, Output>>();");
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
