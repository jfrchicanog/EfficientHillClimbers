package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverForRBallHillClimber;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class PXScoresAlgorithm2Experiment implements Process {

	private long seed;
	private long initTime;
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private double bestSoFar;
    private int r;
    private int populationSize;
    private int time;
    private String n;
    private String k;
    private String q;
    private String circular;

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "px2";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: "
				+ getID()
				+ " <n> <k> <q> <circular> <r> <population size> <time(s)> [<seed>]";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 7) {
			System.out.println(getInvocationInfo());
			return;
		}

		parseArguments(args);

		initializeOutput();
		writeHeader();
		runAlgorithm();	
		printOutput();

	}

    private void runAlgorithm() {
        NKLandscapes pbf = getProblemInstance();
		RBallEfficientHillClimberForInstanceOf rballfio = getHillClimber(pbf);
		PartitionCrossoverForRBallHillClimber px = getCrossover(pbf);
		
	    bestSoFar = -Double.MAX_VALUE;
		initTime = System.currentTimeMillis();

		List<RBallEfficientHillClimberSnapshot> population = initializePopulation(rballfio);
		Comparator<RBallEfficientHillClimberSnapshot> comparator = new Comparator<RBallEfficientHillClimberSnapshot>() {
            @Override
            public int compare(RBallEfficientHillClimberSnapshot o1,
                    RBallEfficientHillClimberSnapshot o2) {
                return Double.compare(o2.getSolutionQuality(), o1.getSolutionQuality());
            }
    
        };
				
		while (timeAvailable()) {

			// Create a generation-0 solution
		    RBallEfficientHillClimberSnapshot newSolution = createGenerationZeroSolution(rballfio);
			notifyExploredSolution(newSolution);

			if (!timeAvailable()) break;
			
			// Apply PX over all the other solutions in the population
			
			List<RBallEfficientHillClimberSnapshot> auxiliarPopulation = new ArrayList<RBallEfficientHillClimberSnapshot>();
			for (RBallEfficientHillClimberSnapshot solution: population) {
			    RBallEfficientHillClimberSnapshot result = px.recombine(solution, newSolution);

	            if (result != null) {
	                hillClimb(result);
	                notifyExploredSolution(result);
	                auxiliarPopulation.add(result);
	            }
	            if (!timeAvailable()) break;
			}
			
			population.addAll(auxiliarPopulation);
            population.sort(comparator);        
            population.subList(populationSize, population.size()).clear();
			
		}
    }

    private boolean timeAvailable() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - initTime) < time * 1000;
    }

    private List<RBallEfficientHillClimberSnapshot> initializePopulation(
            RBallEfficientHillClimberForInstanceOf rballfio) {
        List<RBallEfficientHillClimberSnapshot> population = new ArrayList<RBallEfficientHillClimberSnapshot>();
		for (int i=0; i < populationSize; i++) {
		    RBallEfficientHillClimberSnapshot currentSolution = createGenerationZeroSolution(rballfio);
		    population.add(currentSolution);
		    notifyExploredSolution(currentSolution);
		}
        return population;
    }

    private PartitionCrossoverForRBallHillClimber getCrossover(NKLandscapes pbf) {
        PartitionCrossoverForRBallHillClimber px = new PartitionCrossoverForRBallHillClimber(
				pbf);
		px.setSeed(seed);
        return px;
    }

    private RBallEfficientHillClimberForInstanceOf getHillClimber(NKLandscapes pbf) {
        return (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(
				r).initialize(pbf);
    }

    private void writeHeader() {
        ps.println("N: " + n);
		ps.println("K: " + k);
		ps.println("Q: " + q);
		ps.println("Adjacent model?: "
				+ (circular.equals("y") ? "true" : "false"));
		ps.println("Population size: " + populationSize);
		ps.println("R: " + r);
		ps.println("Seed: " + seed);
    }

    private NKLandscapes getProblemInstance() {
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);

        if (!q.equals("-")) {
            prop.setProperty(NKLandscapes.Q_STRING, q);
        }

        if (circular.equals("y")) {
            prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        }
		
		NKLandscapes pbf = new NKLandscapes();
		pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }

    private void parseArguments(String[] args) {
        n = args[0];
		k = args[1];
		q = args[2];
		circular = args[3];

		r = Integer.parseInt(args[4]);
		populationSize = Integer.parseInt(args[5]);
		time = Integer.parseInt(args[6]);
		seed = 0;
		if (args.length > 7) {
			seed = Long.parseLong(args[7]);
		} else {
			seed = Seeds.getSeed();
		}
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
		ps.println("Elapsed Time: " + (System.currentTimeMillis() - initTime));
		if (quality > bestSoFar) {
			bestSoFar = quality;
			ps.println("* Best so far solution");
		}
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
		double imp;
		do {
			imp = rball.move();
		} while (imp > 0);
	}

}
