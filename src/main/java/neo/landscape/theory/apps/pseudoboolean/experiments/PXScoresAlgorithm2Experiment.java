package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverForRBallHillClimber;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class PXScoresAlgorithm2Experiment implements Process {
    
    private class ScatterSearchLikeStrategy implements SearchStrategy {

        @Override
        public void search() {

            List<RBallEfficientHillClimberSnapshot> auxiliarPopulation = new ArrayList<RBallEfficientHillClimberSnapshot>();

            while (timeAvailable()) {

                do {
                    auxiliarPopulation.clear();

                    for (int i=0; i < population.size() && timeAvailable(); i++){
                        for (int j=i+1; j < population.size() && timeAvailable(); j++) {
                            RBallEfficientHillClimberSnapshot child = px.recombine(population.get(i), population.get(j));
                            if (child != null) {
                                hillClimb(child);
                                notifyExploredSolution(child);
                                auxiliarPopulation.add(child);
                            }
                        }
                    }

                    population.addAll(auxiliarPopulation);
                    population.sort(comparator);
                    population.subList(populationSize, population.size()).clear();


                } while (!auxiliarPopulation.isEmpty() && timeAvailable());

                if (!timeAvailable()) break;
                
                population.subList(populationSize/2, population.size()).clear();

                ps.println("Re-initializing half of population");
                
                while (population.size() < populationSize) {
                    RBallEfficientHillClimberSnapshot newSolution = createGenerationZeroSolution(rballfio);
                    notifyExploredSolution(newSolution);

                    if (!timeAvailable()) break;
                }

            }

        }
        
        @Override
        public String description() {
            return "Scatter Search strategy";
        }

    }

    private interface SearchStrategy {
        public void search();
        public String description();
    }
    
    private class GALikeStrategy implements SearchStrategy {
        
        private Random rnd = new Random(seed);
        private int randomWalkLength;

        @Override
        public void search() {
            
            randomWalkLength = rballfio.getProblem().getN()/4;
            
            int [] positions = new int [population.size()];
            for (int i = 0; i < positions.length; i++) {
                positions[i]=i;
            }
            
            List<RBallEfficientHillClimberSnapshot> auxiliarPopulation = new ArrayList<RBallEfficientHillClimberSnapshot>();

            while (timeAvailable()) {
                
                int indexFirstParent = rnd.nextInt(population.size());
                int firstParent = positions[indexFirstParent];
                
                positions[indexFirstParent] = positions[0];
                positions[0] = firstParent;
                
                int secondParent = positions[rnd.nextInt(population.size()-1)+1];

                RBallEfficientHillClimberSnapshot parent = population.get(firstParent);
                RBallEfficientHillClimberSnapshot child = px.recombine(parent, population.get(secondParent));
                
                if (!timeAvailable()) break;
                
                if (child == null) {
                    PBSolution solution = new PBSolution(parent.getSolution());
                    child = new RBallEfficientHillClimberSnapshot(parent.getHillClimberForInstanceOf(), solution);
                    child.softRestart(randomWalkLength);
                }
                
                if (!timeAvailable()) break;
                
                hillClimb(child);
                notifyExploredSolution(child);
                
                if (!timeAvailable()) break;
                
                population.add(child);
                population.sort(comparator);
                population.subList(populationSize, population.size()).clear();

            }

            
        }

        @Override
        public String description() {
            return "GA-like Search Strategy";
        }
        
    }
    
    private class NewWithPopulationSearchStrategy implements SearchStrategy { 
        /* (non-Javadoc)
         * @see neo.landscape.theory.apps.pseudoboolean.experiments.SearchStrategy#search()
         */
        @Override
        public void search() {
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

        @Override
        public String description() {
            return "New with Population Search Strategy";
        }
    
    }
    
    

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
    private RBallEfficientHillClimberForInstanceOf rballfio;
    private PartitionCrossoverForRBallHillClimber px;
    private List<RBallEfficientHillClimberSnapshot> population;
    private Comparator<RBallEfficientHillClimberSnapshot> comparator;
    private SearchStrategy searchStrategy;

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
				+ " <n> <k> <q> <circular> <r> (new (with population) | ss (scatter search) "
				+ "| ga (like) ) <population size> <time(s)> [<seed>]";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 7) {
			System.out.println(getInvocationInfo());
			return;
		}

		initializeStatistics();
		
		parseArguments(args);

		initializeOutput();
		writeHeader();
		runAlgorithm();	
		writeFooter();
		
		printOutput();

	}

    private void initializeStatistics() {
        bestSoFar = -Double.MAX_VALUE;
        initTime = System.currentTimeMillis();
    }

    private void runAlgorithm() {
        NKLandscapes pbf = getProblemInstance();
		rballfio = getHillClimber(pbf);
		px = getCrossover(pbf);

		ps.println("Search starts: "+(System.currentTimeMillis()-initTime));
		
		
		population = initializePopulation(rballfio);
		comparator = new Comparator<RBallEfficientHillClimberSnapshot>() {
            @Override
            public int compare(RBallEfficientHillClimberSnapshot o1,
                    RBallEfficientHillClimberSnapshot o2) {
                return Double.compare(o2.getSolutionQuality(), o1.getSolutionQuality());
            }
    
        };
				
        searchStrategy.search();
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
		ps.println("Search Strategy: " + searchStrategy.description());
		ps.println("Population size: " + populationSize);
		ps.println("R: " + r);
		ps.println("Seed: " + seed);
    }
    
    private void writeFooter() {
        ps.println("Best so far solution: " + bestSoFar);
        
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
		searchStrategy = parseSearchStrategy(args[5]);
		populationSize = Integer.parseInt(args[6]);
		time = Integer.parseInt(args[7]);
		seed = 0;
		if (args.length > 8) {
			seed = Long.parseLong(args[8]);
		} else {
			seed = Seeds.getSeed();
		}
    }

    private SearchStrategy parseSearchStrategy(String strategy) {
        SearchStrategy searchStrategy=null;
        if (strategy.equals("new")) {
		    searchStrategy = new NewWithPopulationSearchStrategy();
		} else if (strategy.equals("ss")) {
		    searchStrategy = new ScatterSearchLikeStrategy();
		} else if (strategy.equals("ga")) {
		    searchStrategy = new GALikeStrategy();
		}
        return searchStrategy;
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

	private static void hillClimb(RBallEfficientHillClimberSnapshot rball) {
		double imp;
		do {
			imp = rball.move();
		} while (imp > 0);
	}

}
