package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.zip.GZIPOutputStream;

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
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSATConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.ArticulationPointsPartitionCrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.CrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.CrossoverInternal;
import neo.landscape.theory.apps.pseudoboolean.px.DynasticPotentialCrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.NetworkCrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.RBallCrossover;
import neo.landscape.theory.apps.pseudoboolean.px.RBallCrossoverAdaptor;
import neo.landscape.theory.apps.pseudoboolean.px.SinglePointCrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.UniformCrossoverConfigurator;
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
    
    private static final String DPX="dpx";
    private static final String APX="apx";
    private static final String PX="px";
    private static final String NX="nx";
    private static final String UX="ux";
    private static final String SPX="spx";
    private static final String CROSSOVER_NONE = "none";
    
    private static final String TYPE_PERTURBATION="perturbation";
    private static final String TYPE_CROSSOVER="crossover";
    
    private final Map<String, EmbeddedLandscapeConfigurator> configurators = new HashMap<>();
    {
    	configurators.put(MAXSAT_PROBLEM, new MAXSATConfigurator());
    	configurators.put(NK_PROBLEM, new NKLandscapeConfigurator());
    }
    
    private final Map<String, CrossoverConfigurator> crossoverConf = new HashMap<>();
    {
    	crossoverConf.put(DPX, new DynasticPotentialCrossoverConfigurator());
    	crossoverConf.put(APX, new ArticulationPointsPartitionCrossoverConfigurator());
    	crossoverConf.put(PX, new PartitionCrossoverConfigurator());
    	crossoverConf.put(UX, new UniformCrossoverConfigurator());
    	crossoverConf.put(SPX, new SinglePointCrossoverConfigurator());
    	crossoverConf.put(NX, new NetworkCrossoverConfigurator());
    }
     
    
    private long seed;
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private double bestSoFar;
	private Timer timer;

    private int moves;
    private int numberOfExploredSolutions=0;

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
			
			if (commandLine.hasOption(EXPLORED_SOLUTIONS)) {
				final int maxExploredSolutions = Integer.parseInt(commandLine.getOptionValue(EXPLORED_SOLUTIONS));
				shouldIStop = shouldIStop.or(x->numberOfExploredSolutions >= maxExploredSolutions);
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

			try {
				RBallEfficientHillClimberSnapshot currentSolution = createGenerationZeroSolution(rballfio);
				notifyExploredSolution(currentSolution);

				int perturbMoves=20;

				while (!shouldIStop.test(null)) {               
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

					if (px!= null && !shouldIStop.test(null)) {
						child = px.recombine(currentSolution, nextSolution);
					}

					if (child == null) {
						child = nextSolution;
					} else {
						ps.println("* Child different from parents");
						hillClimb(child);
						reportLONEdge(currentSolution, child, TYPE_CROSSOVER);
						reportLONEdge(nextSolution, child, TYPE_CROSSOVER);

						notifyExploredSolution(child);
					}
					currentSolution = acceptanceCriterion(currentSolution, child);
				}
			} catch (Exception e) {
				ps.println("Exception: "+e.getMessage());
				e.printStackTrace(ps);
			}

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
        ps.close();
        try {
            System.out.write(ba.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyExploredSolution(RBallEfficientHillClimberSnapshot exploredSolution) {
        double quality = exploredSolution.getSolutionQuality();
        numberOfExploredSolutions++;
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
            } while (!shouldIStop.test(null));
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
