package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
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
import neo.landscape.theory.apps.util.SingleThreadCPUTimer;

public class EvolutionaryAlgorithmExperiment implements Process {
	
	private static final String DEBUG_ARGUMENT = "debug";
    private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String PROBLEM="problem";
    private static final String CROSSOVER="crossover";
    private static final String POPULATION_SIZE="population";
    private static final String MUTATION_PROB = "mutationProb";
    private static final String CROSSOVER_CHAR = "X";
    private static final String PROBLEM_CHAR = "P";
    
    private static final String MAXSAT_PROBLEM = "maxsat";
    private static final String NK_PROBLEM = "nk";
    
    private static final String DPX="dpx";
    private static final String APX="apx";
    private static final String PX="px";
    private static final String NX="nx";
    private static final String UX="ux";
    private static final String SPX="spx";
   
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
	private SingleThreadCPUTimer timer;

    private Options options;
    
    private CommandLine commandLine;
    private EmbeddedLandscapeConfigurator problemConfigurator;
    private String problem;
    private CrossoverConfigurator crossoverConfigurator;
    private String crossover;
    
    private PBSolution [] population;
    private double [] fitness;
    
    private Random rnd;
	private int worstIndex;

    
	@Override
	public String getDescription() {
		return "Implementation of a basic Evolutionary Algorithm";
	}

    @Override
    public String getID() {
        return "ea";
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
	   
	    options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
	    options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
        options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
        options.addOption(POPULATION_SIZE, true, "number of solution in the population");
        options.addOption(MUTATION_PROB, true, "bit flip mutation probability");
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

			timer = new SingleThreadCPUTimer();
			timer.startTimer();

			initializeStatistics();
			initializeOutput();

			problem = commandLine.getOptionValue(PROBLEM);
			crossover = commandLine.getOptionValue(CROSSOVER);

			EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(
					commandLine.getOptionProperties(PROBLEM_CHAR), ps);

			boolean debug = commandLine.hasOption(DEBUG_ARGUMENT);

			CrossoverInternal px = getCrossoverConfigurator().configureCrossover(
					commandLine.getOptionProperties(CROSSOVER_CHAR), pbf, ps);

			px.setSeed(seed);
			px.setPrintStream(ps);


			if (debug) {
				StringWriter sr = new StringWriter();
				if (pbf instanceof NKLandscapes) {
					((NKLandscapes)pbf).writeTo(sr);
					ps.print(sr.toString());
				}
			}

			int time = Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
			seed = 0;
			if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
				seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
			} else {
				seed = Seeds.getSeed();
			}

			rnd = new Random (seed);

			pbf.setSeed(rnd.nextLong());

			int popSize = Integer.parseInt(commandLine.getOptionValue(POPULATION_SIZE));
			double mutationProbability = Double.parseDouble(commandLine.getOptionValue(MUTATION_PROB));

			population = new PBSolution[popSize];
			fitness = new double [popSize];

			ps.println("Seed: " + seed);
			ps.println("Crossover: " + crossover);
			ps.println("Population size: "+popSize);
			ps.println("Mutation probability: "+mutationProbability);


			timer.setStopTimeMilliseconds(time * 1000);
			ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());

			try {
				// Initialize population
				worstIndex=-1;
				double worstFitness = Double.MAX_VALUE;
				for (int i=0; i < population.length; i++) {
					population[i] = pbf.getRandomSolution();
					fitness[i] = pbf.evaluate(population[i]);
					if (fitness[i] < worstFitness) {
						worstFitness = fitness[i];
						worstIndex = i;
					}
					notifyExploredSolution(population[i], fitness[i]);
				}
				// Main loop of EA
				while (!timer.shouldStop()) {

					PBSolution red  = tournamentSelection();
					PBSolution blue = tournamentSelection();

					PBSolution child = px.recombine(blue, red);

					bitFlipMutation(mutationProbability, child);

					double value = pbf.evaluate(child);
					
					notifyExploredSolution(child, value);

					replacement(child, value);
				}

			} catch (Exception e) {
				ps.println("Exception: "+e.getMessage());
				e.printStackTrace(ps);
			}

			printOutput();

		}

		catch (RuntimeException e) {
			System.err.println("Exception: "+e.getMessage());
			e.printStackTrace(System.err);
			showOptions();
		}

	}

	protected void replacement(PBSolution child, double value) {
		if (value > fitness[worstIndex]) {
			population[worstIndex] = child;
			fitness[worstIndex] = value;
			searchForWorstIndex(value);
		}
	}
	
	protected void searchForWorstIndex(double value) {
		double worstFitness;
		worstFitness = value;
		for (int i=0; i < population.length; i++) {
			if (fitness[i] < worstFitness) {
				worstIndex = i;
				worstFitness = fitness[i];
			}
		}
	}

	protected void bitFlipMutation(double mutationProbability, PBSolution child) {
		for (int v=0; v < child.getN(); v++) {
			if (rnd.nextDouble() < mutationProbability) {
				child.flipBit(v);
			}
		}
	}

	private PBSolution tournamentSelection() {
		int index1 = rnd.nextInt(population.length);
		int index2 = rnd.nextInt(population.length);
		
		if (fitness[index1] > fitness[index2]) {
			return population[index1];
		} else {
			return population[index2];
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

    private void notifyExploredSolution(PBSolution exploredSolution, double quality) {
        ps.println("Solution quality: " + quality);
        ps.println("Elapsed Time: " + timer.elapsedTimeInMilliseconds());
        if (quality > bestSoFar) {
            bestSoFar = quality;
            ps.println("* Best so far solution");
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
