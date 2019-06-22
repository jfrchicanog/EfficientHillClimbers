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


// We need to fix this test, since it does not reflect the new wasy of expressing the NK Model
@Ignore
public class EfficientHillClimberTest {

    public static class Output {
        public int moves;
        public int [] histogram;
        public double quality;
    };

    private Map<Integer, Map<Long, Output>> randomN, randomR, adjacentN, adjacentR, randomMoves;
    private Output o;
    private Map<Long, Output> mapSeed;

    public EfficientHillClimberTest() {
        loadAdjacentRValues();
        loadAdjacentNValues();
        loadRandomRValues();
        loadRandomNValues();
        loadRandomMovesValues();
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
        o.moves=5723;
        o.quality=732183.0;
        o.histogram= new int [] {0, 4990, 733};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=5655;
        o.quality=726725.0;
        o.histogram= new int [] {0, 4945, 710};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=5909;
        o.quality=730705.0;
        o.histogram= new int [] {0, 5143, 766};
        mapSeed.put(20L,o);
        adjacentR.put(2,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6159;
        o.quality=740405.0;
        o.histogram= new int [] {0, 5122, 788, 249};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6076;
        o.quality=735858.0;
        o.histogram= new int [] {0, 5045, 767, 264};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6298;
        o.quality=738430.0;
        o.histogram= new int [] {0, 5261, 808, 229};
        mapSeed.put(20L,o);
        adjacentR.put(3,mapSeed);
        }

        private void loadAdjacentNValues() {
        adjacentN = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=563;
        o.quality=72959.0;
        o.histogram= new int [] {0, 496, 67};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=584;
        o.quality=73072.0;
        o.histogram= new int [] {0, 515, 69};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=559;
        o.quality=73534.0;
        o.histogram= new int [] {0, 494, 65};
        mapSeed.put(20L,o);
        adjacentN.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1154;
        o.quality=145850.0;
        o.histogram= new int [] {0, 1008, 146};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1190;
        o.quality=147218.0;
        o.histogram= new int [] {0, 1037, 153};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1119;
        o.quality=146218.0;
        o.histogram= new int [] {0, 994, 125};
        mapSeed.put(20L,o);
        adjacentN.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=2805;
        o.quality=365424.0;
        o.histogram= new int [] {0, 2446, 359};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=2887;
        o.quality=362050.0;
        o.histogram= new int [] {0, 2496, 391};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=2812;
        o.quality=365778.0;
        o.histogram= new int [] {0, 2415, 397};
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
        o.moves=6229;
        o.quality=727838.0;
        o.histogram= new int [] {0, 5670, 559};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6154;
        o.quality=725404.0;
        o.histogram= new int [] {0, 5639, 515};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6366;
        o.quality=729246.0;
        o.histogram= new int [] {0, 5795, 571};
        mapSeed.put(20L,o);
        randomR.put(2,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=6652;
        o.quality=734145.0;
        o.histogram= new int [] {0, 5760, 702, 190};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=6569;
        o.quality=733210.0;
        o.histogram= new int [] {0, 5817, 599, 153};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=6828;
        o.quality=735290.0;
        o.histogram= new int [] {0, 5965, 675, 188};
        mapSeed.put(20L,o);
        randomR.put(3,mapSeed);
        }

        private void loadRandomNValues() {
        randomN = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=675;
        o.quality=73067.0;
        o.histogram= new int [] {0, 615, 60};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=575;
        o.quality=72684.0;
        o.histogram= new int [] {0, 534, 41};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=639;
        o.quality=73544.0;
        o.histogram= new int [] {0, 594, 45};
        mapSeed.put(20L,o);
        randomN.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1229;
        o.quality=145273.0;
        o.histogram= new int [] {0, 1113, 116};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1182;
        o.quality=144595.0;
        o.histogram= new int [] {0, 1061, 121};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1203;
        o.quality=144290.0;
        o.histogram= new int [] {0, 1099, 104};
        mapSeed.put(20L,o);
        randomN.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=3096;
        o.quality=363286.0;
        o.histogram= new int [] {0, 2811, 285};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=3071;
        o.quality=362482.0;
        o.histogram= new int [] {0, 2813, 258};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=3149;
        o.quality=363894.0;
        o.histogram= new int [] {0, 2898, 251};
        mapSeed.put(20L,o);
        randomN.put(5000,mapSeed);
        }

        private void loadRandomMovesValues() {
        randomMoves = new HashMap<Integer, Map<Long, Output>>();
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=631;
        o.quality=72682.0;
        o.histogram= new int [] {0, 580, 51};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=597;
        o.quality=73610.0;
        o.histogram= new int [] {0, 549, 48};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=623;
        o.quality=73561.0;
        o.histogram= new int [] {0, 568, 55};
        mapSeed.put(20L,o);
        randomMoves.put(1000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=1225;
        o.quality=146199.0;
        o.histogram= new int [] {0, 1120, 105};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=1137;
        o.quality=144544.0;
        o.histogram= new int [] {0, 1029, 108};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=1201;
        o.quality=144759.0;
        o.histogram= new int [] {0, 1079, 122};
        mapSeed.put(20L,o);
        randomMoves.put(2000,mapSeed);
        mapSeed = new HashMap<Long, Output>();
        o = new Output();
        o.moves=2942;
        o.quality=363704.0;
        o.histogram= new int [] {0, 2641, 301};
        mapSeed.put(0L,o);
        o = new Output();
        o.moves=3029;
        o.quality=362321.0;
        o.histogram= new int [] {0, 2737, 292};
        mapSeed.put(10L,o);
        o = new Output();
        o.moves=3046;
        o.quality=365111.0;
        o.histogram= new int [] {0, 2762, 284};
        mapSeed.put(20L,o);
        randomMoves.put(5000,mapSeed);
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
    
    @Test
    public void testRandomChangingNWithRandomMoves() {
        int K=2;
        int Q=100;
        int r=2;
        boolean circular = false;
        String variable = "randomMoves";
        
        printPreambleIfNecessary(variable, randomMoves);
        for (int N: new int [] {1000, 2000, 5000}) {
            creaMapSeedIfNecessary(randomMoves);
            for (long seed: new long [] {0, 10, 20} ) {
                createOutputIfNecessary(randomMoves);
                runAndCheckConfigurationWithRandomMoves(seed, r, N, K, Q, circular, (randomMoves==null)?null:randomMoves.get(N).get(seed));
                putInMapSeedIfNecessary(seed, randomMoves);
            }
            putInMapIfNecessary(variable, N, randomMoves);
        }
        printEpilogueIfNecessary(variable, randomMoves);
    }
    
    @Test
    public void testGetMovementNoException() {
        int K=2;
        int Q=100;
        int r=2;
        boolean circular = false;
        for (int N: new int [] {1000, 2000, 5000}) {
            for (long seed: new long [] {0, 10, 20} ) {
                NKLandscapes pbf = buildNKLandscape(seed, N, K, Q, circular);

                RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r, seed);
                RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf) rball
                        .initialize(pbf);

                PBSolution pbs = pbf.getRandomSolution();
                RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rballf
                        .initialize(pbs);
                
                RBallPBMove move = rballs.getMovement();
                RBallPBMove secondMove = rballs.getMovement();
                
                Assert.assertNotNull(move);
                Assert.assertSame(move, secondMove);
            }
        }
    }
    
    @Test
    public void testGetMovementException() {
        int K=2;
        int Q=100;
        int r=2;
        boolean circular = false;
        for (int N: new int [] {1000, 2000, 5000}) {
            for (long seed: new long [] {0, 10, 20} ) {
                NKLandscapes pbf = buildNKLandscape(seed, N, K, Q, circular);

                RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r, seed);
                RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf) rball
                        .initialize(pbf);

                PBSolution pbs = pbf.getRandomSolution();
                RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rballf
                        .initialize(pbs);
                
                double imp;
                try {
                    do {
                        imp = rballs.move();
                    } while (imp > 0);
                    Assert.fail ("Exception not launched");
                } catch (NoImprovingMoveException e) {

                }
                
                Assert.assertNull(rballs.getMovement());

            }
        }
    }
    
    @Test
    public void testNeutralMoves() {
        int N = 1000;
        int K = 3;
        int Q = 2;
        boolean circular = true;
        long seed = 0;
        int r = 2;
        
        NKLandscapes pbf = buildNKLandscape(seed, N, K, Q, circular);
        
        Properties rballConfig = new Properties();
        rballConfig.setProperty(RBallEfficientHillClimber.NEUTRAL_MOVES, "yes");
        rballConfig.setProperty(RBallEfficientHillClimber.MAX_NEUTRAL_PROBABILITY, "0.5");
        rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+seed);

        RBallEfficientHillClimber rball = new RBallEfficientHillClimber(rballConfig);
        RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf) rball
                .initialize(pbf);

        PBSolution pbs = pbf.getRandomSolution();
        RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rballf
                .initialize(pbs);

        double imp;
        long neutralMoves = 0;
        long moves = 0;
        int plateauMove=0;

        rballs.getStatistics().resetMovesPerDistance();

        try {
            do {
                imp = rballs.move();
                moves++;
                if (imp == 0) {
                    neutralMoves++;
                    plateauMove++;
                    //System.out.println("neutral");
                } else {
                    plateauMove = 0;
                }
                
            } while (plateauMove < 20);
        } catch (NoImprovingMoveException e) {

        }
        //System.out.println("Neutral: "+neutralMoves);
        //System.out.println("TOtal: "+moves);
        Assert.assertTrue(neutralMoves > 0);        
        Assert.assertTrue((neutralMoves-plateauMove)*1.5 <= moves);

    }


    private void runAndCheckConfiguration(long seed, int r, int N, int K, int Q, boolean circular, Output out) {
        NKLandscapes pbf = buildNKLandscape(seed, N, K, Q, circular);

        RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r, seed);
        RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf) rball
                .initialize(pbf);

        PBSolution pbs = pbf.getRandomSolution();
        RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rballf
                .initialize(pbs);

        double imp;
        long moves = 0;

        rballs.getStatistics().resetMovesPerDistance();

        try {
            do {
                imp = rballs.move();
                moves++;
            } while (imp > 0);
            moves--;
        } catch (NoImprovingMoveException e) {

        }

        double final_quality = rballs.getSolutionQuality();

        if (out != null) {

            Assert.assertEquals("Discrepancy in moves (N="+N+", r="+r+", seed="+seed+")", out.moves,  moves);
            Assert.assertArrayEquals("Discrepancy in move histogram (N="+N+", r="+r+", seed="+seed+")", out.histogram, rballs.getStatistics().getMovesPerDistance());
            Assert.assertEquals("Discrepancy in final quality (N="+N+", r="+r+", seed="+seed+")", out.quality,  final_quality, 0.0001);
        }
        else {
            System.out.println("o.moves="+moves+";");
            System.out.println("o.quality="+final_quality+";");
            String histogram = Arrays.toString(rballs.getStatistics().getMovesPerDistance());
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


    protected NKLandscapes buildNKLandscape(long seed, int N, int K, int Q, boolean circular) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, ""+K);
        prop.setProperty(NKLandscapes.Q_STRING, ""+Q);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular?"yes":"no");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }
    
    private void runAndCheckConfigurationWithRandomMoves(long seed, int r, int N, int K, int Q, boolean circular, Output out) {
        NKLandscapes pbf = buildNKLandscape(seed, N, K, Q, circular);

        Properties configuration = new Properties();
        configuration.setProperty(RBallEfficientHillClimber.R_STRING, "" + r);
        configuration.setProperty(RBallEfficientHillClimber.SEED, ""+seed);
        configuration.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
        
        
        RBallEfficientHillClimber rball = new RBallEfficientHillClimber(configuration);
        RBallEfficientHillClimberForInstanceOf rballf = (RBallEfficientHillClimberForInstanceOf) rball
                .initialize(pbf);

        PBSolution pbs = pbf.getRandomSolution();
        RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rballf
                .initialize(pbs);

        double imp;
        long moves = 0;

        rballs.getStatistics().resetMovesPerDistance();

        try {
            do {
                imp = rballs.move();
                moves++;
            } while (imp > 0);
            moves--;
        } catch (NoImprovingMoveException e) {

        }

        double final_quality = rballs.getSolutionQuality();

        if (out != null) {

            Assert.assertEquals("Discrepancy in moves (N="+N+", r="+r+", seed="+seed+")", out.moves,  moves);
            Assert.assertArrayEquals("Discrepancy in move histogram (N="+N+", r="+r+", seed="+seed+")", out.histogram, rballs.getStatistics().getMovesPerDistance());
            Assert.assertEquals("Discrepancy in final quality (N="+N+", r="+r+", seed="+seed+")", out.quality,  final_quality, 0.0001);
        }
        else {
            System.out.println("o.moves="+moves+";");
            System.out.println("o.quality="+final_quality+";");
            String histogram = Arrays.toString(rballs.getStatistics().getMovesPerDistance());
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
