package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverForRBallHillClimber;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.SingleThreadCPUTimer;

public class PartitionCrossoverWithScoresExperiment implements Process {

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
		return "Arguments: "
				+ getID()
				+ " <n> <k> <q> <circular> <seed (problem)> <r> <generation limit> <max Plateau moves> <time(s)> [<seed>]";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 9) {
			System.out.println(getInvocationInfo());
			return;
		}
		
		timer = new SingleThreadCPUTimer();
		timer.startTimer();
		
		initializeStatistics();
		
		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		long problemSeed = Long.parseLong(args[4]);
		int r = Integer.parseInt(args[5]);
		int generationLimit = Integer.parseInt(args[6]);
		maxPlateauMoves = Integer.parseInt(args[7]);
		int time = Integer.parseInt(args[8]);
		seed = 0;
		if (args.length > 9) {
			seed = Long.parseLong(args[9]);
		} else {
			seed = Seeds.getSeed();
		}

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

		initializeOutput();

		ps.println("N: " + n);
		ps.println("K: " + k);
		ps.println("Q: " + q);
		ps.println("Adjacent model?: "
				+ (circular.equals("y") ? "true" : "false"));
		ps.println("Generation limit: " + generationLimit);
		ps.println("R: " + r);
		ps.println("Seed: " + seed);
		ps.println("ProblemSeed: "+problemSeed);

		pbf.setSeed(problemSeed);
		pbf.setConfiguration(prop);
		
		Properties rballConfig = new Properties();
        //rballConfig.setProperty(RBallEfficientHillClimber.NEUTRAL_MOVES, "yes");
        //rballConfig.setProperty(RBallEfficientHillClimber.MAX_NEUTRAL_PROBABILITY, "0.5");
        rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
		rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+seed);

		RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) 
		        new RBallEfficientHillClimber(rballConfig).initialize(pbf);
		Stack<ExploredSolution> explored = new Stack<ExploredSolution>();
		PartitionCrossoverForRBallHillClimber px = new PartitionCrossoverForRBallHillClimber(
				pbf);
		px.setSeed(seed);

		timer.setStopTimeMilliseconds(time * 1000);
		
		crossoverFailsInGeneration = new HashMap<Integer, Integer>();

		ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());

		try {
		    while (!timer.shouldStop()) {
		        // Create a generation-0 solution
		        ExploredSolution currentSolution = createGenerationZeroSolution(rballfio);
		        notifyExploredSolution(currentSolution);

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
		                    && !timer.shouldStop()) {
		                ExploredSolution popedSolution = explored.pop();
		                RBallEfficientHillClimberSnapshot result = px.recombine(
		                        popedSolution.solution, currentSolution.solution);

		                if (result == null) {
		                    increaseCrossoverFailInGeneration(currentSolution.generation);
		                    currentSolution = null;
		                } else {
		                    hillClimb(result);
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
		printOutput();

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

}
