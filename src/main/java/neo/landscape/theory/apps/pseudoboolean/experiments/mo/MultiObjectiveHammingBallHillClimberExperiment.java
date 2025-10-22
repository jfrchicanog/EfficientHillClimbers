package neo.landscape.theory.apps.pseudoboolean.experiments.mo;

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

import neo.landscape.theory.apps.pseudoboolean.problems.mo.*;
import org.apache.commons.cli.*;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveSelector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveSelector.KindOfMove;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.Timer;
import neo.landscape.theory.apps.util.Timers;

public class MultiObjectiveHammingBallHillClimberExperiment implements Process {

	private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String PROBLEM_CHAR = "P";
    private static final String PROBLEM="problem";
    private static final String MNK_PROBLEM = "mnk";
    private static final String MQUBO = "mqubo";

	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private Timer timer;
	
	private Random random;

    private Options options;
    
    ParetoNonDominatedSet nonDominatedSet;
    private int totalMoves;

    private VectorMKLandscapeConfigurator configurator;

    private String problem;

    private final Map<String, VectorMKLandscapeConfigurator> configurators = new HashMap<>();
    {
        configurators.put(MNK_PROBLEM, new MNKLandscapeConfigurator());
        configurators.put(MQUBO, new MquboConfigurator());
    }

    
	@Override
	public String getDescription() {
		return "Multi Objective Hamming Ball Hill Climber";
	}

    @Override
    public String getID() {
        return "mo-hbhc";
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

    private VectorMKLandscapeConfigurator getProblemConfigurator() {
        if (configurator==null) {
            configurator = createProblemConfigurator();
        }
        return configurator;
    }

    private VectorMKLandscapeConfigurator createProblemConfigurator() {
        VectorMKLandscapeConfigurator elc =  configurators.get(problem);
        if (elc == null) {
            throw new IllegalArgumentException("Problem "+problem+" is unknown");
        }
        return elc;
    }
	
	private Options prepareOptions() {
        Options options = new Options();

	    options.addOption(RADIUS_ARGUMENT, true, "radius of the Hamming Ball hill climber");
	    options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
	    options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
        options.addOption(PROBLEM, true, "problem to be solved: "+configurators.keySet());
        options.addOption(Option.builder(PROBLEM_CHAR)
            .numberOfArgs(2)
            .valueSeparator()
            .argName("property=value")
            .desc("properties for the problem")
            .build());
	    
	    return options;
	}

	@Override
	public void execute(String[] args) {

		if (args.length == 0) {
            showOptions();
            return;
		}

        try {

            CommandLine commandLine = parseCommandLine(args);

            timer = Timers.getDefaultTimer();
            timer.startTimer();

            problem = commandLine.getOptionValue(PROBLEM);

            initializeDataHolders();
            initializeOutput();

            VectorMKLandscape pbf = getProblemConfigurator().configureProblem(
                commandLine.getOptionProperties(PROBLEM_CHAR), ps);

            int r = Integer.parseInt(commandLine.getOptionValue(RADIUS_ARGUMENT));
            int time = Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));

            timer.setStopTimeMilliseconds(time * 1000);

            long seed = 0;
            if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
                seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
            } else {
                seed = Seeds.getSeed();
            }

            ps.println("R: " + r);
            ps.println("Seed: " + seed);

            random = new Random(seed);

            Properties rballConfig = new Properties();

            rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
            rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r + "");
            rballConfig.setProperty(RBallEfficientHillClimber.SEED, "" + seed);

            MultiObjectiveHammingBallHillClimberForInstanceOf rballfio =
                (MultiObjectiveHammingBallHillClimberForInstanceOf) new MultiObjectiveHammingBallHillClimber(rballConfig).initialize(pbf);

            ps.println("Search starts: " + timer.elapsedTimeInMilliseconds());


            while (!timer.shouldStop()) {
                double[] weights = generateRandomPositiveWeights(pbf.getDimension());
                PBSolution solution = pbf.getRandomSolution();

                MultiObjectiveHammingBallHillClimberSnapshot rball = rballfio.initialize(weights, solution);
                rball.setSeed(random.nextLong());

                totalMoves += hillClimb(rball);
            }

            ps.println("Total moves: " + totalMoves);
            ps.println("Stored scores:" + rballfio.getStoredScores());
            ps.println("Total time (ms):" + timer.elapsedTimeInMilliseconds());
            ps.println("Average time per move (nanoseconds):" + timer.elapsedTime() / (double) totalMoves);
            ps.println(nonDominatedSet.printArchive());

            printOutput();
        } catch (Exception e) {
            showOptions();
        }
    }

    private void showOptions() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(getID(), getOptions());

        try {
            Options problemOptions = new Options();
            getProblemConfigurator().prepareOptionsForProblem(problemOptions);
            helpFormatter.printHelp("Problem: "+problem, problemOptions);
        } catch (RuntimeException e) {
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

    private void initializeDataHolders() {
        totalMoves = 0;
        nonDominatedSet = new ParetoNonDominatedSet();
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


    private double[] generateRandomPositiveWeights(int dimension) {
        double [] weights = new double [dimension];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = random.nextDouble();
            if (weights[i] == 0.0) {
                weights[i] = 0.000001;
            }
        }
        return weights;
    }

    private int hillClimb(MultiObjectiveHammingBallHillClimberSnapshot rball) {
        MultiObjectiveSelector selector = (MultiObjectiveSelector)rball.getMovesSelector();
        int moves=0;
        try {
            do {
                VectorPBMove move = rball.getMovement();
                KindOfMove kind = selector.classifyMove(move);
                if (KindOfMove.W_IMPROVING.equals(kind)) {
                    nonDominatedSet.reportSolutionToArchive(rball.getSolutionQuality());
                }
                rball.move();
                //rball.checkConsistency();
                moves++;
            } while (!timer.shouldStop());
        } catch (NoImprovingMoveException e) {
        }
        nonDominatedSet.reportSolutionToArchive(rball.getSolutionQuality());
        return moves;
    }    

}
