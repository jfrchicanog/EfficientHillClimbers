package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverForRBallHillClimber;
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

public class PartitionCrossoverWithScoresExperiment implements Process {
    
    protected static final String MOVES_IN_PLATEAU = "mp";
    private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String TARGET_OBJECTIVE_VALUE = "target";
    private static final String GENERATION_LIMIT = "genlimit";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String LON_ARGUMENT = "lon";
    private static final String LON_MINIMUM_FITNESS_ARGUMENT = "lonmin";
    

	private static class ExploredSolution {
		public RBallEfficientHillClimberSnapshot solution;
		public int generation;

		public static ExploredSolution createExploredSolution(
				RBallEfficientHillClimberSnapshot solution, int generation) {
			ExploredSolution res = new ExploredSolution();
			res.generation = generation;
			res.solution = solution;
			return res;
		}

	}

	private long seed;
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private double bestSoFar;
	private Map<Integer, Integer> crossoverFailsInGeneration;
	private SingleThreadCPUTimer timer;
	private int maxPlateauMoves;
    private EmbeddedLandscapeConfigurator problemConfigurator;
    protected CommandLine commandLine;
    private Options options;
    private Double targetObjectiveValue;
    private boolean targetReached;
	
    private Graph graph;
    private PBSolutionDigest solutionDigest;
    private Double localOptimumFitnessFilter;

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "px";
	}

	@Override
	public String getInvocationInfo() {
	    HelpFormatter helpFormatter = new HelpFormatter();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        helpFormatter.printUsage(printWriter, Integer.MAX_VALUE, getID(), getOptions());
        return stringWriter.toString();
	}

	@Override
	public void execute(String[] args) {
	    if (args.length == 0) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp(getID(), getOptions());
            return;
        }
        commandLine = parseCommandLine(args);
		
		initializeOutput();
		initializeStatistics();
		initializeTimer();
		initializeTargetObjectiveValue();

		if (targetObjectiveValue==null && !timer.isStopSet()) {
		    throw new IllegalArgumentException("A stop condition must be set with "
		            +TIME_ARGUMENT+" or "+TARGET_OBJECTIVE_VALUE+" options");
		}

		EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(commandLine, ps);
		
		if (commandLine.hasOption(LON_ARGUMENT)) {
		    initializeLONDataStructures(pbf);
		}
		
		int r = Integer.parseInt(commandLine.getOptionValue(RADIUS_ARGUMENT));
		int generationLimit = Integer.parseInt(commandLine.getOptionValue(GENERATION_LIMIT));
		
		maxPlateauMoves = Integer.parseInt(commandLine.getOptionValue(MOVES_IN_PLATEAU));
		
		seed = 0;
        if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
            seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
        } else {
            seed = Seeds.getSeed();
        }

		ps.println("Generation limit: " + generationLimit);
		ps.println("R: " + r);
		ps.println("Seed: " + seed);
		
		Properties rballConfig = new Properties();
        rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
		rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+seed);

		RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) 
		        new RBallEfficientHillClimber(rballConfig).initialize(pbf);
		Stack<ExploredSolution> explored = new Stack<ExploredSolution>();
		PartitionCrossoverForRBallHillClimber px = new PartitionCrossoverForRBallHillClimber(
				pbf);
		px.setSeed(seed);

		
		crossoverFailsInGeneration = new HashMap<Integer, Integer>();

		ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());
		targetReached = false;

		try {
		    while (!timer.shouldStop() && !targetReached) {
		        // Create a generation-0 solution
		        ExploredSolution currentSolution = createGenerationZeroSolution(rballfio);
		        notifyExploredSolution(currentSolution);
		        if (targetReached) {
		            continue;
		        }

		        if (explored.empty()
		                || explored.peek().generation > currentSolution.generation) {
		            if (currentSolution.generation < generationLimit) {
		                explored.push(currentSolution);
		            }
		        } else {
		            // Recombine solutions with the same level
		            while ((!explored.empty())
		                    && currentSolution != null
		                    && explored.peek().generation == currentSolution.generation
		                    && !timer.shouldStop()
		                    && !targetReached) {
		                ExploredSolution popedSolution = explored.pop();
		                RBallEfficientHillClimberSnapshot result = px.recombine(
		                        popedSolution.solution, currentSolution.solution);

		                if (result == null) {
		                    increaseCrossoverFailInGeneration(currentSolution.generation);
		                    currentSolution = null;
		                } else {
		                    hillClimb(result);
		                    
		                    reportLONEdge(popedSolution.solution, result);
                            reportLONEdge(currentSolution.solution, result);    
                            
		                    currentSolution = ExploredSolution
		                            .createExploredSolution(result,
		                                    currentSolution.generation + 1);
		                    notifyExploredSolution(currentSolution);
		                }

		            }

		            if (currentSolution != null) {
		                if (currentSolution.generation < generationLimit) {
		                    explored.push(currentSolution);
		                }
		            }
		        }

		    }
		} catch (Exception e) {
		    ps.print("Exception: "+e.getMessage());
		}

		writeCrossoverFails();
		writeLONInformation();
		printOutput();

	}

    private void reportLONEdge(RBallEfficientHillClimberSnapshot solution,
            RBallEfficientHillClimberSnapshot result) {
        if (graph != null) {
            if (localOptimumFitnessFilter==null || 
                    (solution.getSolutionQuality() >= localOptimumFitnessFilter &&
                    result.getSolutionQuality() >= localOptimumFitnessFilter)) {
                graph.addEdge(solutionDigest.getHashOfSolution(solution), 
                        solutionDigest.getHashOfSolution(result), "");
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

    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        return new NKLandscapeConfigurator();
    }

    private void initializeStatistics() {
        bestSoFar = -Double.MAX_VALUE;
    }

	private void writeCrossoverFails() {
		List<Integer> generations = new ArrayList<Integer>();
		generations.addAll(crossoverFailsInGeneration.keySet());
		Collections.sort(generations);
		for (int generation : generations) {
			ps.println("Crossover fails in generation " + generation + ": "
					+ crossoverFailsInGeneration.get(generation));
		}

	}
	
	private void writeLONInformation() {
        if (graph != null) {
            ps.println("LON nodes:"+graph.printNodes());
            ps.println("LON edges:"+graph.printEdges());
        }
    }

	private void increaseCrossoverFailInGeneration(int generation) {
		Integer fails = crossoverFailsInGeneration.get(generation);
		if (fails == null) {
			fails = 0;
		}
		crossoverFailsInGeneration.put(generation, fails + 1);

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

	private void notifyExploredSolution(ExploredSolution exploredSolution) {
		double quality = exploredSolution.solution.getSolutionQuality();
		ps.println("Generation level:" + exploredSolution.generation);
		ps.println("Solution quality: " + quality);
		ps.println("Elapsed Time: " + timer.elapsedTimeInMilliseconds());
		if (quality > bestSoFar) {
			bestSoFar = quality;
			ps.println("* Best so far solution");
		}
		
		reportLONNode(exploredSolution.solution);
		
		targetReached = (targetObjectiveValue != null && bestSoFar >= targetObjectiveValue);
	}

	private ExploredSolution createGenerationZeroSolution(
			RBallEfficientHillClimberForInstanceOf rballfio) {
		RBallEfficientHillClimberSnapshot rball = rballfio.initialize(rballfio
				.getProblem().getRandomSolution());
		rball.setSeed(seed);
		hillClimb(rball);

		return ExploredSolution.createExploredSolution(rball, 0);
	}

	private void hillClimb(RBallEfficientHillClimberSnapshot rball) {
	    double imp;
	    int plateauMoves =0;
	    try {
	        do {
	            imp = rball.move();
	            if (imp == 0) {
	                plateauMoves++;
	            } else  if (imp > 0) {
	                plateauMoves =0;
	            } else {
	                throw new RuntimeException ("Negative move!");
	            }
	        } while (plateauMoves <= maxPlateauMoves);
	    } catch (NoImprovingMoveException e) {

	    }
	}
	
	private CommandLine parseCommandLine(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            return parser.parse(getOptions(), args);
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }
	
	private Options getOptions() {
        if (options == null) {
            options = prepareOptions();
        }
        return options;
    }
    
    protected Options prepareOptions() {
        Options options = new Options();
        
        getProblemConfigurator().prepareOptionsForProblem(options);
        options.addOption(RADIUS_ARGUMENT, true, "radius of the Hamming Ball hill climber");
        options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
        options.addOption(TARGET_OBJECTIVE_VALUE, true, "target objective value");
        options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
        options.addOption(MOVES_IN_PLATEAU, true, "maximum number of moves in a plateau");
        options.addOption(GENERATION_LIMIT, true, "maximum generation limit");
        options.addOption(LON_ARGUMENT,false, "print the PX Local Optima Network");
        options.addOption(LON_MINIMUM_FITNESS_ARGUMENT,true, "minimum fitness to consider a LON (optional)");
        
        return options;
    }

    private EmbeddedLandscapeConfigurator getProblemConfigurator() {
        if (problemConfigurator==null) {
            problemConfigurator = createEmbeddedLandscapeConfigurator();
        }
        return problemConfigurator;
    }
    
    private void initializeTargetObjectiveValue() {
        if (commandLine.hasOption(TARGET_OBJECTIVE_VALUE)) {
            targetObjectiveValue = Double.parseDouble(commandLine.getOptionValue(TARGET_OBJECTIVE_VALUE));
        }
    }

    protected void initializeTimer() {
        timer = new SingleThreadCPUTimer();
        timer.startTimer();
        if (commandLine.hasOption(TIME_ARGUMENT)) {
            int time = Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
            timer.setStopTimeMilliseconds(time * 1000);
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
