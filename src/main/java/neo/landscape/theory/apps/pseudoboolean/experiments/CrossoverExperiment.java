package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
import neo.landscape.theory.apps.pseudoboolean.px.SinglePointCrossoverConfigurator;
import neo.landscape.theory.apps.pseudoboolean.px.UniformCrossoverConfigurator;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.Timer;
import neo.landscape.theory.apps.util.Timers;

public class CrossoverExperiment implements Process {
	
	private static final String DEBUG_ARGUMENT = "debug";
    private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIMER_ARGUMENT="timer";
    private static final String TIME_ARGUMENT = "time";
    private static final String EXPLORED_SOLUTIONS = "expSols";
    private static final String HAMMING_DISTANCE_ARGUMENT = "hd";
    private static final String PROBLEM="problem";
    private static final String CROSSOVER="crossover";
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
	private Timer timer;

    private int numberOfCrossovers=0;

    private Options options;
    
    private CommandLine commandLine;
    private EmbeddedLandscapeConfigurator problemConfigurator;
    private String problem;
    private CrossoverConfigurator crossoverConfigurator;
    private String crossover;
	private Predicate<?> shouldIStop;
	
	private int [] variableToFlip;
	private Random rnd;

    
	@Override
	public String getDescription() {
		return "Crossover experiment";
	}

    @Override
    public String getID() {
        return "crossover";
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
	    
	    options.addOption(HAMMING_DISTANCE_ARGUMENT, true, "Fraction of variables differing between the parent solutions");
	    options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
	    options.addOption(TIMER_ARGUMENT, true, "timer to use ["+Timers.SINGLE_THREAD_CPU+","+Timers.CPU_CLOCK+"], default: "+Timers.getNameOfDefaultTimer());
	    options.addOption(EXPLORED_SOLUTIONS, true, "explored solutions limit");
	    options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
        options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
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

			initializeOutput();

			problem = commandLine.getOptionValue(PROBLEM);
			crossover = commandLine.getOptionValue(CROSSOVER);
			
			EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(
					commandLine.getOptionProperties(PROBLEM_CHAR), ps);
			
			boolean debug = commandLine.hasOption(DEBUG_ARGUMENT);

			CrossoverInternal ci = getCrossoverConfigurator().configureCrossover(
					commandLine.getOptionProperties(CROSSOVER_CHAR), pbf, ps);
			ci.setSeed(seed);
			ci.setPrintStream(ps);

			if (debug) {
				StringWriter sr = new StringWriter();
				if (pbf instanceof NKLandscapes) {
					((NKLandscapes)pbf).writeTo(sr);
					ps.print(sr.toString());
				}
			}
			
			configureStoppingCriterion();
			configureAlgorithmSeed();
			
			rnd = new Random(seed);
			final int n = pbf.getN();
			initializeVariableToFlip(n);

			final double perturbFactor = Double.parseDouble(commandLine.getOptionValue(HAMMING_DISTANCE_ARGUMENT));
			final int perturbMoves = (int)(perturbFactor*n);
			
			ps.println("Perturbation factor: " + perturbFactor);
			ps.println("Seed: " + seed);
			ps.println("Crossover: " + crossover);
			ps.println("Experiment starts: "+timer.elapsedTimeInMilliseconds());

			while (!shouldIStop.test(null)) {  
				PBSolution redParent = pbf.getRandomSolution();
				PBSolution blueParent = getSecondParent(perturbMoves, redParent);
				final double redQuality = pbf.evaluate(redParent);
				ps.println("Red parent quality: "+redQuality);
				final double blueQuality = pbf.evaluate(blueParent);
				ps.println("Blue parent quality: "+blueQuality);
				PBSolution child = ci.recombine(blueParent, redParent);
		        numberOfCrossovers++;
				if (child != null) {
					ps.println("* Child different from parents");
				}
				final double childQuality = pbf.evaluate(child);
				ps.println("Child quality: "+childQuality);
				final double bestParentQuality = Math.max(redQuality, blueQuality);
				final double qualityImprovement = (childQuality-bestParentQuality)/bestParentQuality;
				ps.println("Quality improvement (%): "+qualityImprovement);
			}
			
			ps.println("Experiment ends: "+timer.elapsedTimeInMilliseconds());
			
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

	protected PBSolution getSecondParent(final int perturbMoves, PBSolution redParent) {
		PBSolution blueParent = new PBSolution(redParent);
		for (int flip=0; flip < perturbMoves; flip++) {
			blueParent.flipBit(getNextVarToFlip(flip));
		}
		return blueParent;
	}

	protected int getNextVarToFlip(int flip) {
		int index = rnd.nextInt(variableToFlip.length-flip);
		int var = variableToFlip[index+flip];
		variableToFlip[index+flip]=variableToFlip[flip];
		variableToFlip[flip] = var;
		return var;
	}

	protected void initializeVariableToFlip(final int n) {
		variableToFlip = new int [n];
		for (int i=0; i < n; i++) {
			variableToFlip[i]=i;
		}
	}

	protected void configureAlgorithmSeed() {
		seed = 0;
		if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
			seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
		} else {
			seed = Seeds.getSeed();
		}
	}

	protected void configureStoppingCriterion() {
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
			shouldIStop = shouldIStop.or(x->numberOfCrossovers >= maxExploredSolutions);
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
