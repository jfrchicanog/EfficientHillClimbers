package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
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
    private EmbeddedLandscape pbf;
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
    
    private String outputFileName;

    @Override
    public String getDescription() {
        return "This experiment computes the local optima exploring the search  "
        + "space using Goldman's algorithm";
    }

    @Override
    public String getID() {
        return "lon-goldman";
    }

    protected long findLocalOptima() {
        int n = getPbf().getN();
        int index = n-1;
        long solutions=0;
        long nextSolutionsReport = solutions + REPORT_PERIOD;
        
        initializeVariableArrays();
        initializeMoveBin();
        initializeLONDataStructures();
        //counter = new int[n];
        
        preparePrefix();
        
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
        return localOptima;
    }
    
    private void initializeLONDataStructures() {
    	
        localOptima = new ArrayList<>();
    }

    private void storeLocalOptima() {
        localOptima.add(new PBSolution(rball.getSolution()));
    }

    private void preparePrefix() {
        int index=getPbf().getN()-1;
        for (char c: prefix.toCharArray()) {
            if (c == '1') {
                rball.moveOneBit(variableOrder[index]);
            }
            index--;
        }
    }

    private void initializeVariableArrays() {
        int n = getPbf().getN();
        int [][] interactions = getPbf().getInteractions();
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
        moveBin = new List[getPbf().getN()];
        for (int i = 0; i < moveBin.length; i++) {
            moveBin[i] = new ArrayList<RBallPBMove>();
        }
        for (RBallPBMove move: rball.iterateOverMoves()) {
            int min = Integer.MAX_VALUE;
            for (int subfn : rballfio.subFunctionsAffected(move.flipVariables)) {
                int [] mask = getPbf().getMasks()[subfn];
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
                r).initialize(getPbf());
        PBSolution pbs = new PBSolution(getPbf().getN());
        
        rball = rballfio.initialize(pbs);
        rball.setSeed(seed);
    }

    @Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + " <list-of-lo.gz> (nk <n> <k> <q> <circular> <r> [<seed> [<prefix>]] | maxsat <instance> <r> [<seed> [<prefix>]])";
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
            setPbf(configureNKInstance(args));
        } else if ("maxsat".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            setPbf(configureMaxsatInstance(args));
        }
        
        if (getPbf() == null) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        if (!checkPrefixOK()) {
            System.err.println("The prefix must be a binary string");
            return;
        }
        
        System.out.println("Seed: "+seed);
        
        prepareRBallExplorationAlgorithm();
        long localOptima = findLocalOptima();  
        
        System.out.println("Local optima: "+localOptima);
        System.out.println("Writing in file "+outputFileName);
        
        writeLOInFile();
        
        System.out.println("written");
        
        if (getPbf() instanceof NKLandscapes) {
            reportNKInstanceToStandardOutput();
        }

        
    }

    private void writeLOInFile() {
    	try (FileOutputStream fos = new FileOutputStream(outputFileName);
    		 GZIPOutputStream gzos = new GZIPOutputStream(fos);
    		 PrintWriter writer = new PrintWriter(gzos)) 
    	{
    		localOptima.forEach(solution->{
    			writer.println(solution.toHex()+" "+getPbf().evaluate(solution));
    		});
    		
    		
    	} catch (IOException e) {
			throw new RuntimeException(e);
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
        ((NKLandscapes)getPbf()).writeTo(new OutputStreamWriter(System.out));
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

	protected EmbeddedLandscape getPbf() {
		return pbf;
	}

	protected void setPbf(EmbeddedLandscape pbf) {
		this.pbf = pbf;
	}

}
