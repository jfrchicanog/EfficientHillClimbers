package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
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

public class LocalOptimaNetworkGoldman implements Process {
    
    private static final long REPORT_PERIOD = 1L<<30;
    protected EmbeddedLandscape pbf;
    protected int r;
    protected RBallEfficientHillClimberSnapshot rball;
    protected RBallEfficientHillClimberForInstanceOf rballfio;
    protected long seed;
    protected String prefix="";

    protected List<RBallPBMove> [] moveBin; 
    protected long counterValue;
    protected int [] variableOrder;
    protected int [] variableRank;
    //protected int [] counter;
    protected List<PBSolution> localOptima;
    private HillClimberForInstanceOf<EmbeddedLandscape> rballHillClimber;
    private String outputFileName;

    @Override
    public String getDescription() {
        return "This experiment computes the local optima network exploring the search  "
        + "space using Goldman's algorithm";
    }

    @Override
    public String getID() {
        return "lon-goldman";
    }

    protected long findLocalOptima() {
        int n = pbf.getN();
        int index = n-1;
        long solutions=0;
        long nextSolutionsReport = solutions + REPORT_PERIOD;
        
        initializeVariableArrays();
        initializeMoveBin();
        initializeLONDataStructures();
        //counter = new int[n];
        
        preparPrefix();
        
        long localOptima = 0;
        
        int limitIndex = n-prefix.length();
        
        index = findNextIndex(index);
        if (index >= limitIndex) {
            solutions += (1L << limitIndex);
        }
        
        while (index < limitIndex) {
            index = findNextIndex(index);
            if (index < 0) {
                localOptima++;
                storeLocalOptima();
                index = 0;
            }
            solutions += (1L << index);
            if (solutions > nextSolutionsReport) {
                System.out.println("Solutions explored: "+(double)solutions);
                System.out.println("Local optima: "+localOptima);
                nextSolutionsReport = solutions + REPORT_PERIOD;
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
                rball.moveOneBit(variableOrder[index]);
            }
            
        }
        System.out.println("Total solutions explored: "+solutions);
        lonExtraction();
        return localOptima;
    }

    private void lonExtraction() {
        Properties properties = new Properties();
        properties.setProperty(RBallEfficientHillClimber.R_STRING, ""+r);
        properties.setProperty(RBallEfficientHillClimber.SEED, ""+seed);
        rballHillClimber = new RBallEfficientHillClimber(properties).initialize(pbf);
        
        try (OutputStream file = new FileOutputStream(outputFileName); 
             OutputStream out = new GZIPOutputStream(file); 
             PrintWriter writer = new PrintWriter(out)) {
            
            extractLON(writer);
                    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void extractLON(PrintWriter writer) { 
        writer.println("{");
        for (int i=0; i < localOptima.size(); i++) {
            PBSolution solution = localOptima.get(i);
            writer.println(String.format("\"%s\": {", solution.toHex()));
            Map<PBSolution, Long> histogram = computeLocalOptimaReachability(solution);
            AtomicLong written = new AtomicLong(0);
            histogram.entrySet().stream().forEach(entry -> {
                writer.print(String.format("\t\"%s\": %d", entry.getKey().toHex(), entry.getValue()));
                if (written.incrementAndGet() < histogram.size()) {
                    writer.print(",");
                }
                writer.println();
                
            });
            writer.print("}");
            if (i < localOptima.size()-1) {
                writer.print(",");
            }
            writer.println();
        }
        writer.println("}");        
    }

    private Map<PBSolution, Long> computeLocalOptimaReachability(PBSolution solution) {
        Map<PBSolution, Long> histogram = new HashMap<>();
        int n = solution.getN();
        // Hamming distance 2 neighborhood
        for (int i =0; i < n-1; i ++) {
            for (int j=i+1; j < n; j++) {
                PBSolution neighbor = new PBSolution(solution);
                neighbor.flipBit(i);
                neighbor.flipBit(j);
                
                
                RBallEfficientHillClimberSnapshot rball = (RBallEfficientHillClimberSnapshot) rballHillClimber.initialize(neighbor);
                climbToLocalOptima(rball);
                histogram.compute(rball.getSolution(), (k,v)->v==null?1:(v+1));
            }
        }
        return histogram;
    }
    
    private void climbToLocalOptima(RBallEfficientHillClimberSnapshot rball) {
        double imp;
        do {
            imp = rball.move();

        } while (imp > 0);
    }

    private void initializeLONDataStructures() {
        localOptima = new ArrayList<>();
        
    }

    private void storeLocalOptima() {
        localOptima.add(new PBSolution(rball.getSolution()));
    }

    private void preparPrefix() {
        int index=pbf.getN()-1;
        for (char c: prefix.toCharArray()) {
            if (c == '1') {
                rball.moveOneBit(variableOrder[index]);
            }
            index--;
        }
    }

    private void initializeVariableArrays() {
        int n = pbf.getN();
        int [][] interactions = pbf.getInteractions();
        variableOrder = IntStream.range(0, n)
                   .boxed()
                   //.sorted(Comparator.comparingInt(i->interactions[i].length))
                   .mapToInt(i->i).toArray(); 
        variableRank = new int[n];
        for (int i=0; i < n; i++) {
            variableRank[variableOrder[i]]=i;
        }
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
                    if (variableRank[var] < min) {
                        min = variableRank[var];
                    }
                }
            }
            
            moveBin[min].add(move);
            System.out.println("Move "+move+" in "+min);
        }
        for (int i=0; i < moveBin.length; i++) {
            System.out.println("Bin["+i+"]: "+moveBin[i].size());
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
        return "Arguments: " + getID() + " <output.json.gz> (nk <n> <k> <q> <circular> <r> [<seed> [<prefix>]] | maxsat <instance> <r> [<seed> [<prefix>]])";
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        outputFileName = args[0];
        args= Arrays.copyOfRange(args, 1, args.length);

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
        
        if (!checkPrefixOK()) {
            System.err.println("The prefix must be a binary string");
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

    private boolean  checkPrefixOK() {
        return prefix.chars().allMatch(c->(c >= '0' && c <= '1'));
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
        
        if (args.length >= 7) {
            prefix = args[6];
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
        
        if (args.length >= 4) {
            prefix = args[3];
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
