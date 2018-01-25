package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes.NKModel;
import neo.landscape.theory.apps.pseudoboolean.px.PXAPForRBallHillClimber;
import neo.landscape.theory.apps.util.Graph;
import neo.landscape.theory.apps.util.PBSolutionDigest;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.SingleThreadCPUTimer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ILSRBallPXAPExperiment implements Process {


	private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String MOVES_FACTOR_ARGUMENT = "mf";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String PROBLEM_SEED_ARGUMENT = "pseed";
    private static final String MODEL_ARGUMENT = "model";
    private static final String Q_ARGUMENT = "q";
    private static final String K_ARGUMENT = "k";
    private static final String N_ARGUMENT = "n";
    private static final String LON_ARGUMENT = "lon";
    private static final String LON_MINIMUM_FITNESS_ARGUMENT = "lonmin";
    
    private static final String TYPE_PERTURBATION="perturbation";
    private static final String TYPE_CROSSOVER="crossover";
    
    private long seed;
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private double bestSoFar;
	private SingleThreadCPUTimer timer;

    private int moves;

    private Options options;
    
    private Graph graph;
    private PBSolutionDigest solutionDigest;
    private Double localOptimumFitnessFilter;
    private CommandLine commandLine;

    
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public String getID() {
        return "rball+ils+pxap";
    }

	@Override
	public String getInvocationInfo() {
	    HelpFormatter helpFormatter = new HelpFormatter();
	    StringWriter stringWriter = new StringWriter();
	    PrintWriter printWriter = new PrintWriter(stringWriter);

	    helpFormatter.printUsage(printWriter, Integer.MAX_VALUE, getID(), getOptions());
	    return stringWriter.toString();
	}

    private Options getOptions() {
        if (options == null) {
            options = prepareOptions();
        }
        return options;
    }
	
	private Options prepareOptions() {
	    Options options = new Options();
	    
	    options.addOption(N_ARGUMENT, true, "number of variables");
	    options.addOption(K_ARGUMENT, true, "number of subfunction arguments");
	    options.addOption(Q_ARGUMENT, true, "cardinality of subfunction domain");
	    options.addOption(MODEL_ARGUMENT, true, "NK-model: y->adjacent, n->random, <number>->Localized");
	    options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
	    options.addOption(RADIUS_ARGUMENT, true, "radius of the Hamming Ball hill climber");
	    options.addOption(MOVES_FACTOR_ARGUMENT, true, "proportion of variables used for the random walk in the perturbation");
	    options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
	    options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
	    options.addOption(LON_ARGUMENT,false, "print the PX Local Optima Network");
        options.addOption(LON_MINIMUM_FITNESS_ARGUMENT,true, "minimum fitness to consider a LON (optional)");
	    
	    return options;
	}



	@Override
	public void execute(String[] args) {
	    
		if (args.length == 0) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(getID(), getOptions());
			return;
		}
		
		commandLine = parseCommandLine(args);
		
		timer = new SingleThreadCPUTimer();
		timer.startTimer();
		
		initializeStatistics();
		initializeOutput();
		
		NKLandscapes pbf = configureProblem(commandLine);
		
		if (commandLine.hasOption(LON_ARGUMENT)) {
            initializeLONDataStructures(pbf);
        }
		
		int r = Integer.parseInt(commandLine.getOptionValue(RADIUS_ARGUMENT));
		double perturbFactor;
		if ("-".equals(commandLine.getOptionValue(MOVES_FACTOR_ARGUMENT))) {
		    perturbFactor = -1;
		} else {
		    perturbFactor = Double.parseDouble(commandLine.getOptionValue(MOVES_FACTOR_ARGUMENT));
		}
		
		int time = Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
		seed = 0;
		if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
			seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
		} else {
			seed = Seeds.getSeed();
		}
		
		ps.println("Perturbation factor: " + perturbFactor);
		ps.println("R: " + r);
		ps.println("Seed: " + seed);
		

		Properties rballConfig = new Properties();

        //rballConfig.setProperty(RBallEfficientHillClimber.NEUTRAL_MOVES, "yes");
        //rballConfig.setProperty(RBallEfficientHillClimber.MAX_NEUTRAL_PROBABILITY, "0.5");
        rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
        rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+seed);

        RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) 
                new RBallEfficientHillClimber(rballConfig).initialize(pbf);
        PXAPForRBallHillClimber px = new PXAPForRBallHillClimber(pbf);
        px.setSeed(seed);

        timer.setStopTimeMilliseconds(time * 1000);
        ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());

        try {
            RBallEfficientHillClimberSnapshot currentSolution = createGenerationZeroSolution(rballfio);
            notifyExploredSolution(currentSolution);
            
            int perturbMoves=20;
            
            while (!timer.shouldStop()) {               
                RBallEfficientHillClimberSnapshot nextSolution = rballfio.initialize(new PBSolution(currentSolution.getSolution()), currentSolution);
                
                if (perturbFactor < 0) {
                    if (moves > 0) {
                        perturbMoves=moves;
                    }
                } else {
                    perturbMoves = (int)(perturbFactor*pbf.getN());
                }
                
                nextSolution.softRestart(perturbMoves);
                ps.println("* Hamming distance after perturbation: "+nextSolution.getSolution().hammingDistance(currentSolution.getSolution()));
                hillClimb(nextSolution);
                notifyExploredSolution(nextSolution);
                reportLONEdge(currentSolution, nextSolution, TYPE_PERTURBATION);

		        RBallEfficientHillClimberSnapshot child = null;
		        
		        if (!timer.shouldStop()) {
		            child = px.recombine(currentSolution, nextSolution);
		        }
		        
		        if (child == null) {
		            currentSolution = nextSolution;
		        } else {
		            ps.println("* Success in PX: "+px.getNumberOfComponents());
		            ps.println("* Articulation Points: "+px.getNumberOfArticulationPoints());
		            hillClimb(child);
		            reportLONEdge(currentSolution, child, TYPE_CROSSOVER);
		            reportLONEdge(nextSolution, child, TYPE_CROSSOVER);
		            
		            currentSolution = child;
		            notifyExploredSolution(currentSolution);
		        }
		        
		    }
		} catch (Exception e) {
		    ps.print("Exception: "+e.getMessage());
		}

        writeLONInformation();
        printOutput();

    }

    private NKLandscapes configureProblem(CommandLine commandLine) {
        String n = commandLine.getOptionValue(N_ARGUMENT);
        String k = commandLine.getOptionValue(K_ARGUMENT);
        String q = commandLine.getOptionValue(Q_ARGUMENT);
        String circular = commandLine.getOptionValue(MODEL_ARGUMENT);
        long problemSeed = Long.parseLong(commandLine.getOptionValue(PROBLEM_SEED_ARGUMENT));
        
		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);

		if (!q.equals("-")) {
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}

		if (circular.equals("y")) {
			prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
		} else {
		    prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
		}
		
		pbf.setSeed(problemSeed);
        pbf.setConfiguration(prop);

		ps.println("N: " + pbf.getN());
		ps.println("K: " + pbf.getK());
		ps.println("Q: " + pbf.getQ());
		ps.println("Adjacent model?: "
				+ (NKModel.ADJACENT.equals(pbf.getNKModel()) ? "true" : "false"));
		ps.println("NK-model: "+circular);
		ps.println("ProblemSeed: "+problemSeed);
        return pbf;
    }

    private CommandLine parseCommandLine(String[] args) {
		try {
		    CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }

    private void initializeStatistics() {
        bestSoFar = -Double.MAX_VALUE;
    }

    private void initializeOutput() {
        ba = new ByteArrayOutputStream();
        try {
            ps = new PrintStream(new GZIPOutputStream(ba));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printOutput() {
        ps.close();
        try {
            System.out.write(ba.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyExploredSolution(RBallEfficientHillClimberSnapshot exploredSolution) {
        double quality = exploredSolution.getSolutionQuality();
        ps.println("Solution quality: " + quality);
        ps.println("Elapsed Time: " + timer.elapsedTimeInMilliseconds());
        ps.println("* Moves: "+moves);
        if (quality > bestSoFar) {
            bestSoFar = quality;
            ps.println("* Best so far solution");
        }
        
        reportLONNode(exploredSolution);
    }

    private RBallEfficientHillClimberSnapshot createGenerationZeroSolution(
            RBallEfficientHillClimberForInstanceOf rballfio) {
        RBallEfficientHillClimberSnapshot rball = rballfio.initialize(rballfio
                .getProblem().getRandomSolution());
        rball.setSeed(seed);
        hillClimb(rball);

        return rball;
    }

    private void hillClimb(RBallEfficientHillClimberSnapshot rball) {
        moves=0;
        try {
            do {
                rball.move();
                moves++;
            } while (!timer.shouldStop());
        } catch (NoImprovingMoveException e) {

        }
    }
    
    private void reportLONEdge(RBallEfficientHillClimberSnapshot solution,
            RBallEfficientHillClimberSnapshot result, String kind) {
        if (graph != null) {
            if (localOptimumFitnessFilter==null || 
                    (solution.getSolutionQuality() >= localOptimumFitnessFilter &&
                    result.getSolutionQuality() >= localOptimumFitnessFilter)) {
                graph.addEdge(solutionDigest.getHashOfSolution(solution), 
                        solutionDigest.getHashOfSolution(result), kind);
            }
        }
    }
    
    private void reportLONNode(RBallEfficientHillClimberSnapshot solution) {
        if (graph != null) {
            if (localOptimumFitnessFilter==null || solution.getSolutionQuality() >= localOptimumFitnessFilter) {
                graph.addNode(solutionDigest.getHashOfSolution(solution), solution.getSolutionQuality());
            }
        }
    }
    
    private void writeLONInformation() {
        if (graph != null) {
            ps.println("LON nodes:"+graph.printNodes());
            ps.println("LON edges:"+graph.printEdges());
        }
    }
    
    protected void initializeLONDataStructures(EmbeddedLandscape el) {
        graph = new Graph();
        solutionDigest = new PBSolutionDigest(el.getN());
        if (commandLine.hasOption(LON_MINIMUM_FITNESS_ARGUMENT)) {
            localOptimumFitnessFilter = Double.parseDouble(commandLine.getOptionValue(LON_MINIMUM_FITNESS_ARGUMENT));
        }
    }


}
