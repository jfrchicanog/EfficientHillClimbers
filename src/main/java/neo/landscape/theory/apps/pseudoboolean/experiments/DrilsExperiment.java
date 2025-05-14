package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.problems.*;
import neo.landscape.theory.apps.pseudoboolean.problems.AnkRnkLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.util.Graph;
import neo.landscape.theory.apps.util.PBSolutionDigest;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.Timer;
import neo.landscape.theory.apps.util.Timers;

public class DrilsExperiment implements Process {
	
	private static final String DEBUG_ARGUMENT = "debug";
    private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String OUT_FILE = "outfile";
    private static final String EXPLORED_SOLUTIONS = "expSols";
    private static final String MOVES_FACTOR_ARGUMENT = "mf";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String LON_ARGUMENT = "lon";
    private static final String LON_MINIMUM_FITNESS_ARGUMENT = "lonmin";
    private static final String IMPROVING_LO = "improvingLo";
    private static final String PROBLEM="problem";
    private static final String CROSSOVER="crossover";
    private static final String CROSSOVER_CHAR = "X";
    private static final String PROBLEM_CHAR = "P";
    private static final String TIMER_ARGUMENT="timer";
    
    private static final String MAXSAT_PROBLEM = "maxsat";
    private static final String NK_PROBLEM = "nk";
    private static final String NKSAT_PROBLEM = "nksat";
    private static final String ANK_RNK_PROBLEM = "ank_rnk";
	private static final String WALSH_PROBLEM = "walsh";
    
    private static final String DPX="dpx";
    private static final String APX="apx";
    private static final String PX="px";
    private static final String NX="nx";
    private static final String UX="ux";
    private static final String SPX="spx";
	private static final String FPX="fpx";
	private static final String FDPX="fdpx";
    private static final String CROSSOVER_NONE = "none";
    
    private static final String TYPE_PERTURBATION="perturbation";
    private static final String TYPE_CROSSOVER="crossover";
    private String filename="output.gz";

    private final Map<String, EmbeddedLandscapeConfigurator> configurators = new HashMap<>();
    {
    	configurators.put(MAXSAT_PROBLEM, new MAXSATConfigurator());
    	configurators.put(NK_PROBLEM, new NKLandscapeConfigurator());
    	configurators.put(NKSAT_PROBLEM, new NKSATLandscapeConfigurator());
    	configurators.put(ANK_RNK_PROBLEM, new AnkRnkLandscapeConfigurator());
		configurators.put(WALSH_PROBLEM, new WalshBasedFunctionConfigurator());
    }
    
    private final Map<String, CrossoverConfigurator> crossoverConf = new HashMap<>();
    {
    	crossoverConf.put(DPX, new DynasticPotentialCrossoverConfigurator());
    	crossoverConf.put(APX, new ArticulationPointsPartitionCrossoverConfigurator());
    	crossoverConf.put(PX, new PartitionCrossoverConfigurator());
    	crossoverConf.put(UX, new UniformCrossoverConfigurator());
    	crossoverConf.put(SPX, new SinglePointCrossoverConfigurator());
    	crossoverConf.put(NX, new NetworkCrossoverConfigurator());
		crossoverConf.put(FPX, new FourierPartitionCrossoverConfigurator());
		crossoverConf.put(FDPX, new FourierDynasticPotentialCrossoverConfigurator());
    }
     
    
    private long seed;
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private double bestSoFar;
	private Timer timer;

    private int moves;
    private int numberOfExploredSolutions=0;
    private int climbs=0;

    private Options options;
    
    private Graph graph;
    private PBSolutionDigest solutionDigest;
    private Double localOptimumFitnessFilter;
    private CommandLine commandLine;
    private EmbeddedLandscapeConfigurator problemConfigurator;
    private String problem;
    private CrossoverConfigurator crossoverConfigurator;
    private String crossover;
	private Predicate<?> shouldIStop;

    
	@Override
	public String getDescription() {
		return "Implementation of DRILS";
	}

    @Override
    public String getID() {
        return "drils";
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
	    
	    options.addOption(RADIUS_ARGUMENT, true, "radius of the Hamming Ball hill climber");
	    options.addOption(MOVES_FACTOR_ARGUMENT, true, "proportion of variables used for the random walk in the perturbation");
	    options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
	    options.addOption(OUT_FILE, true, "output file name");
	    options.addOption(TIMER_ARGUMENT, true, "timer to use ["+Timers.SINGLE_THREAD_CPU+","+Timers.CPU_CLOCK+"], default: "+Timers.getNameOfDefaultTimer());
	    options.addOption(EXPLORED_SOLUTIONS, true, "explored solutions limit");
	    options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
	    options.addOption(LON_ARGUMENT,false, "print the PX Local Optima Network");
        options.addOption(LON_MINIMUM_FITNESS_ARGUMENT,true, "minimum fitness to consider a LON (optional)");
        options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
        options.addOption(IMPROVING_LO, false, "accept only non disimproving local optima in ILS");
        options.addOption(PROBLEM, true, "problem to be solved: "+configurators.keySet());
        options.addOption(CROSSOVER, true, "crossover operator to use: "+crossoverConf.keySet());
        options.addOption(Option.builder(PROBLEM_CHAR)
        		.numberOfArgs(2)
        		.valueSeparator()
        		.argName("property=value")
        		.desc("properties for the problem")
        		.build());

        options.addOption(Option.builder(CROSSOVER_CHAR)
        		.numberOfArgs(2)
        		.valueSeparator()
        		.argName("property=value")
        		.desc("properties for the crossover operator")
        		.build());

        
	    return options;
	}

	@Override
	public void execute(String[] args) {
		try {
			commandLine = parseCommandLine(args);
			
			configureTimer();
			
			timer.startTimer();

			initializeStatistics();
			initializeOutput();

			problem = commandLine.getOptionValue(PROBLEM);
			crossover = commandLine.getOptionValue(CROSSOVER);
			
			EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(
					commandLine.getOptionProperties(PROBLEM_CHAR), ps);
			
			boolean debug = commandLine.hasOption(DEBUG_ARGUMENT);

			RBallCrossover px = null; 
			if (!crossover.equals(CROSSOVER_NONE)) {
				CrossoverInternal ci = getCrossoverConfigurator().configureCrossover(
						commandLine.getOptionProperties(CROSSOVER_CHAR), pbf, ps);
				
				px = new RBallCrossoverAdaptor (ci);
				px.setSeed(seed);
				px.setPrintStream(ps);
			}
			
			if (debug) {
				StringWriter sr = new StringWriter();
				if (pbf instanceof NKLandscapes) {
					((NKLandscapes)pbf).writeTo(sr);
					ps.print(sr.toString());
				}
				if (pbf instanceof SumOfEmbeddedLandscapes) {
					((SumOfEmbeddedLandscapes)pbf).writeTo(sr);
					ps.print(sr);
				}
			}

			if (commandLine.hasOption(LON_ARGUMENT)) {
				initializeLONDataStructures(pbf);
			}
			
			if (!commandLine.hasOption(TIME_ARGUMENT) && !commandLine.hasOption(EXPLORED_SOLUTIONS)) {
				System.err.println("A stopping condition must be set using "+TIME_ARGUMENT+" or "+EXPLORED_SOLUTIONS);
				throw new IllegalArgumentException("A stopping condition must be set using "+TIME_ARGUMENT+" or "+EXPLORED_SOLUTIONS);
			}
			
			shouldIStop = (x -> false);
			if (commandLine.hasOption(TIME_ARGUMENT)) {
				int time = Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
				timer.setStopTimeMilliseconds(time * 1000);
				shouldIStop = shouldIStop.or(x->timer.shouldStop());
			}
			if (commandLine.hasOption(OUT_FILE)) {
				filename = commandLine.getOptionValue(OUT_FILE);
			}
			
			if (commandLine.hasOption(EXPLORED_SOLUTIONS)) {
				final int maxExploredSolutions = Integer.parseInt(commandLine.getOptionValue(EXPLORED_SOLUTIONS));
				shouldIStop = shouldIStop.or(x->numberOfExploredSolutions >= maxExploredSolutions);
				shouldIStop = shouldIStop.or(x->climbs >= maxExploredSolutions);
			}

			int r = Integer.parseInt(commandLine.getOptionValue(RADIUS_ARGUMENT));
			double perturbFactor;
			if ("-".equals(commandLine.getOptionValue(MOVES_FACTOR_ARGUMENT))) {
				perturbFactor = -1;
			} else {
				perturbFactor = Double.parseDouble(commandLine.getOptionValue(MOVES_FACTOR_ARGUMENT));
			}

			
			seed = 0;
			if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
				seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
			} else {
				seed = Seeds.getSeed();
			}

			ps.println("Perturbation factor: " + perturbFactor);
			ps.println("R: " + r);
			ps.println("Seed: " + seed);
			ps.println("Crossover: " + crossover);

			Properties rballConfig = new Properties();

			//rballConfig.setProperty(RBallEfficientHillClimber.NEUTRAL_MOVES, "yes");
			//rballConfig.setProperty(RBallEfficientHillClimber.MAX_NEUTRAL_PROBABILITY, "0.5");
			rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
			rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
			rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+seed);

			RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) 
					new RBallEfficientHillClimber(rballConfig).initialize(pbf);

			
			ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());

			System.out.println("Hill Climbing and Searching is about to start....");
			int countChild = 0;
			int countCrossover = 0;

			try {
				RBallEfficientHillClimberSnapshot previousSolution = createGenerationZeroSolution(rballfio);
				notifyExploredSolution(previousSolution);

				int perturbMoves=20;

				while (!shouldIStop.test(null)) {               
					RBallEfficientHillClimberSnapshot currentSolution = rballfio.initialize(new PBSolution(previousSolution.getSolution()), previousSolution);

					if (perturbFactor < 0) {
						if (moves > 0) {
							perturbMoves=moves;
						}
					} else {
						perturbMoves = (int)(perturbFactor*pbf.getN());
					}

					currentSolution.softRestart(perturbMoves);
					ps.println("* Hamming distance after perturbation: "+currentSolution.getSolution().hammingDistance(previousSolution.getSolution()));
					hillClimb(currentSolution);
					notifyExploredSolution(currentSolution);
					reportLONEdge(previousSolution, currentSolution, TYPE_PERTURBATION);

					System.out.println("Hill climbing done, going to perform crossover operation");
					RBallEfficientHillClimberSnapshot child = null;

					if (px!= null && !shouldIStop.test(null)) {
						child = px.recombine(previousSolution, currentSolution);
						System.out.println("Recombination is done, childId:" + countCrossover);
						countCrossover++;
					}

					if (child == null) {
						child = currentSolution;
					} else {
						countChild++;
						ps.println("* Child different from parents");
						hillClimb(child);
						reportLONEdge(previousSolution, child, TYPE_CROSSOVER);
						reportLONEdge(currentSolution, child, TYPE_CROSSOVER);

						notifyExploredSolution(child);
						System.out.println("Hill climbing done, going to perform next loop");
					}
					previousSolution = acceptanceCriterion(previousSolution, child);
				}
			} catch (Exception e) {
				ps.println("Exception: "+e.getMessage());
				e.printStackTrace(ps);
			}
			ps.println("===============================================================================================");
			ps.println("Execution Time: "+timer.elapsedTimeInMilliseconds());
			ps.println("crossover Operated: "+countCrossover + "times");
			ps.println("No. of children different from parents: "+countChild);

			writeLONInformation();
			printOutput();

		} catch (RuntimeException e) {
			System.err.println("Exception: "+e.getMessage());
			e.printStackTrace(System.err);
			showOptions();
		}

    }

	protected void configureTimer() {
		timer = Timers.getDefaultTimer();
		if (commandLine.hasOption(TIMER_ARGUMENT)) {
			timer = Timers.getTimer(commandLine.getOptionValue(TIMER_ARGUMENT));
		}
	}

	protected void showOptions() {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(getID(), getOptions());
		
		try {
			Options problemOptions = new Options();
			getProblemConfigurator().prepareOptionsForProblem(problemOptions);
			helpFormatter.printHelp("Problem: "+problem, problemOptions);
		} catch (RuntimeException e) {
		}
		
		try {
			Options crossoverOptions = new Options();
			getCrossoverConfigurator().prepareOptionsForCrossover(crossoverOptions);
			helpFormatter.printHelp("Crossover: "+crossover, crossoverOptions);
		} catch (RuntimeException e) {
		}
		return;
	}

	protected RBallEfficientHillClimberSnapshot acceptanceCriterion(
            RBallEfficientHillClimberSnapshot currentSolution, 
            RBallEfficientHillClimberSnapshot child) {
        
        if (commandLine.hasOption(IMPROVING_LO) && currentSolution.getSolutionQuality() > child.getSolutionQuality()) {
            return currentSolution;
        } else {
            return child;
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
        try {
            System.out.write(ba.toByteArray());
			writeCompressedToFile(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void writeCompressedToFile(String filename) {
		ps.close(); // Ensure everything is written to the ByteArrayOutputStream
		try (FileOutputStream fos = new FileOutputStream(filename)) {
			fos.write(ba.toByteArray()); // Write compressed data to a file
			System.out.println("Compressed data written to file: " + filename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    private void notifyExploredSolution(RBallEfficientHillClimberSnapshot exploredSolution) {
		double quality = exploredSolution.getSolutionQuality();
		numberOfExploredSolutions++;
		ps.println("Solution: " + exploredSolution.getSolution().toString());
		ps.println("Solution quality: " + quality);
		if (problem.equals(ANK_RNK_PROBLEM)) {
			// In this problem type, first problem is NK and second one is RNK
			SumOfEmbeddedLandscapes problem = (SumOfEmbeddedLandscapes) exploredSolution.getProblem();
			List<Integer> nValues = problem.getNValues();
			Double[] subfnsEvals = exploredSolution.getSubfnsEvals();
			double nkSolQuality = Arrays.stream(subfnsEvals)
					.limit(nValues.get(0)).mapToDouble(Double::doubleValue).sum();
			ps.println("** ANK Solution quality: " + nkSolQuality);
			ps.println("** RNK Solution quality: " + (quality - nkSolQuality));
		}
		ps.println("Elapsed Time: " + timer.elapsedTimeInMilliseconds());
		ps.println("* Moves: " + moves);
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
		moves = 0;
		climbs = 0;
		try {
			do {
				rball.move();
				moves++;
				climbs++;
			} while (!shouldIStop.test(null));
			climbs = 0;
		} catch (NoImprovingMoveException e) {
			System.out.println("Got No Improving Move Exception");

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
    
    private EmbeddedLandscapeConfigurator getProblemConfigurator() {
        if (problemConfigurator==null) {
            problemConfigurator = createEmbeddedLandscapeConfigurator();
        }
        return problemConfigurator;
    }
    
    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
    	EmbeddedLandscapeConfigurator elc =  configurators.get(problem);
    	if (elc == null) {
    		throw new IllegalArgumentException("Problem "+problem+" is unknown");
    	}
    	return elc;
    }
    
    protected CrossoverConfigurator createCrossoverConfigurator() {
    	CrossoverConfigurator xConf = crossoverConf.get(crossover);
    	if (xConf == null) {
    		throw new IllegalArgumentException("Crossover "+crossover+" is unknown");
    	}
    	return xConf;
    }
    
    private CrossoverConfigurator getCrossoverConfigurator() {
    	if (crossoverConfigurator==null) {
    		crossoverConfigurator = createCrossoverConfigurator();
    	}
    	return crossoverConfigurator;
    }


}
