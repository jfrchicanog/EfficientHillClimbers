package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class LocalOptimaCounting implements Process {
    
    protected EmbeddedLandscape pbf;
    protected int r;
    protected RBallEfficientHillClimberSnapshot rball;
    protected RBallEfficientHillClimberForInstanceOf rballfio;
    protected long seed;

    protected List<RBallPBMove> [] moveBin; 
    protected long counterValue;
    //protected int [] counter;

    @Override
    public String getDescription() {
        return "This experiment computes the All the Local Optima of the search "
        + "space using the Goldman algorithm";
    }

    @Override
    public String getID() {
        return "lo-count";
    }

    protected long findLocalOptima() {
        int n = pbf.getN();
        int index = n-1;
        long solutions=0;
        long nextSolutionsReport = solutions + (1L<<30);
        PBSolution solution = rball.getSolution();
        initializeMoveBin();
        //counter = new int[n];
        
        long localOptima = 0;
        
        while (index < n) {
            index = findNextIndex(index);
            if (index < 0) {
                localOptima++;
                index = 0;
            }
            solutions += (1L << index);
            if (solutions > nextSolutionsReport) {
                System.out.println("Solutions explored: "+(double)solutions);
                System.out.println("Local optima: "+localOptima);
                nextSolutionsReport = solutions + (1L<<30);
            }
            long previousCounterValue = counterValue;
            counterValue += (1L<<index);
            index=63-Long.numberOfLeadingZeros(counterValue & ~previousCounterValue);
            /*
            while (index < n && counter[index] == 1) {
                counter[index]=0;
                //rball.moveOneBit(index);
                index++;
            }*/
            if (index < n) {
                //counter[index]=1;
                rball.moveOneBit(index);
            }
            
        }
        System.out.println("Total solutions explored: "+solutions);
        return localOptima;
    }

    protected int findNextIndex(int index) {
        while (index >= 0) {
            for (RBallPBMove move : moveBin[index]) {
                if (move.improvement > 0) {
                    return index;
                }
            }
            index--;
        }
        return index;
    }
    
    protected void initializeMoveBin() {
        moveBin = new List[pbf.getN()];
        for (int i = 0; i < moveBin.length; i++) {
            moveBin[i] = new ArrayList<RBallPBMove>();
        }
        for (RBallPBMove move: rball.iterateOverMoves()) {
            int min = Integer.MAX_VALUE;
            for (int subfn : rballfio.subFunctionsAffected(move.flipVariables)) {
                int [] mask = pbf.getMasks()[subfn];
                for (int var: mask) {
                    if (var < min) {
                        min = var;
                    }
                }
            }
            
            moveBin[min].add(move);
            System.out.println("Move "+move+" in "+min);
        }

    }

    protected void prepareRBallExplorationAlgorithm() {
        rballfio = (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(
                r).initialize(pbf);
        PBSolution pbs = new PBSolution(pbf.getN());
        
        rball = rballfio.initialize(pbs);
        rball.setSeed(seed);
    }

    @Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + "[nk <n> <k> <q> <circular> <r> [<seed>]] | [maxsat <instance> <r> [<seed>]]";
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }

        if ("nk".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            pbf = configureNKInstance(args);
        } else if ("maxsat".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            pbf = configureMaxsatInstance(args);
        }
        
        if (pbf == null) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        prepareRBallExplorationAlgorithm();

        long localOptima = findLocalOptima();
        System.out.println("Seed: "+seed);
        System.out.println("Local optima: "+localOptima);
        
        if (pbf instanceof NKLandscapes) {
            reportNKInstanceToStandardOutput();
        }

        
    }

    private EmbeddedLandscape configureNKInstance(String[] args) {
        String n = args[0];
        String k = args[1];
        String q = args[2];
        String circular = args[3];
        r = Integer.parseInt(args[4]);
        seed = 0;
        if (args.length >= 6) {
            seed = Long.parseLong(args[5]);
        } else {
            seed = Seeds.getSeed();
        }
        
        return createNKInstance(n, k, q, circular);
    }
    
    private EmbeddedLandscape configureMaxsatInstance(String [] args) {
        String instance = args[0];
        r = Integer.parseInt(args[1]);
        seed = 0;
        if (args.length >= 3) {
            seed = Long.parseLong(args[2]);
        } else {
            seed = Seeds.getSeed();
        }
        Properties prop = new Properties();
        prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
        MAXSAT maxsat = new MAXSAT();
        maxsat.setConfiguration(prop);
        return maxsat;
    }

    private void reportNKInstanceToStandardOutput() {
        ((NKLandscapes)pbf).writeTo(new OutputStreamWriter(System.out));
    }
    
    private EmbeddedLandscape createNKInstance(String n, String k, String q, String circular) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);

        if (!q.equals("-")) {
            prop.setProperty(NKLandscapes.Q_STRING, q);
        }

        if (circular.equals("y")) {
            prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        }

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        
        return pbf;
    }

}
