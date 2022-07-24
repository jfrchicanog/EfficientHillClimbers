package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.tuple.Pair;

import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class LocalOptimaNetworkGoldmanExtraction implements Process {
    
    protected EmbeddedLandscape pbf;
    protected int r;
    protected long seed;

    protected long counterValue;
    private HillClimberForInstanceOf<EmbeddedLandscape> rballHillClimber;
    private String outputFileName;
    private String loFileName;

    @Override
    public String getDescription() {
        return "This experiment computes the local optima network form a list of local optima";
    }

    @Override
    public String getID() {
        return "lon-goldman-extraction";
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
    
    private void printHistogramForSolution(PrintWriter writer, PBSolution solution, Map<PBSolution, Long> histogram) {
    	writer.println(String.format("\"%s\": {", solution.toHex()));
    	AtomicLong written = new AtomicLong(0);
        histogram.entrySet().stream().forEach(entry -> {
            writer.print(String.format("\t\"%s\": %d", entry.getKey().toHex(), entry.getValue()));
            if (written.incrementAndGet() < histogram.size()) {
                writer.print(",");
            }
            writer.println();
            
        });
        writer.print("}");
    }
    
    
    private void extractLON(PrintWriter writer) {
        writer.println("{");
        readLoFile()
        	.map(solution->Pair.of(solution, computeLocalOptimaReachability(solution)))
        	.reduce(null, (p1, p2)->{
        		if (p1!=null) {
        			writer.print(",");
        		}
        		writer.println();
        		printHistogramForSolution(writer, p2.getLeft(), p2.getRight());
        		return p2;
        	});        
        writer.println("}");          
    }

    private Map<PBSolution, Long> computeLocalOptimaReachability(PBSolution solution) {
        Map<PBSolution, Long> histogram = new HashMap<>();
        int n = solution.getN();
        // Hamming distance 2 neighborhood
        for (int i =0; i < n-1; i ++) {
        	//System.out.print(".");
            for (int j=i+1; j < n; j++) {
                PBSolution neighbor = new PBSolution(solution);
                neighbor.flipBit(i);
                neighbor.flipBit(j);
                
                
                RBallEfficientHillClimberSnapshot rball = (RBallEfficientHillClimberSnapshot) rballHillClimber.initialize(neighbor);
                climbToLocalOptima(rball);
                histogram.compute(rball.getSolution(), (k,v)->v==null?1:(v+1));
            }
        }
        //System.out.println();
        return histogram;
    }
    
    private void climbToLocalOptima(RBallEfficientHillClimberSnapshot rball) {
        double imp;
        do {
            imp = rball.move();

        } while (imp > 0);
    }

    @Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + " <output.json.gz> <list-of-lo.gz> (nk <n> <k> <q> <circular> <r> [<seed>] | maxsat <instance> <r> [<seed>])";
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }
        
        outputFileName = args[0];
        loFileName = args[1];
        args= Arrays.copyOfRange(args, 2, args.length);

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
        
        System.out.println("Seed: "+seed);

        lonExtraction();

        if (pbf instanceof NKLandscapes) {
            reportNKInstanceToStandardOutput();
        }

        
    }

    private Stream<PBSolution> readLoFile() {
    	
    	Iterator<PBSolution> iterator = new Iterator<PBSolution>() {
    		private FileInputStream fis;
    		private GZIPInputStream gzis;
    		private Scanner scanner;
			private String line;
    		
    		{
    			try {
    				fis = new FileInputStream(loFileName);
    				gzis = new GZIPInputStream(fis);
    				scanner = new Scanner(gzis);
    			} catch (IOException e) {
    				throw new RuntimeException(e);
    			}
    		}
    		
			@Override
			public boolean hasNext() {
				while (scanner.hasNextLine()) {
    	    		line = scanner.nextLine().trim();
    	    		if (!line.isEmpty()) {
    	    			return true;
    	    		}
    	    	}
				try {
					scanner.close();
					gzis.close();
					fis.close();
				} catch (IOException e) {
				}
    	    	return false;
			}

			@Override
			public PBSolution next() {
				PBSolution solution = new PBSolution(pbf.getN());
				solution.fromHex(line);
				return solution;
			}
    		
    	};
    	
    	return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
		
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
