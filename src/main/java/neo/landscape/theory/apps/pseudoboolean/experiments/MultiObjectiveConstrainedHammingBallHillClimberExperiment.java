package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveHammingBallHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveSelector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.MultiObjectiveSelector.KindOfMove;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.HillClimberListener;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.MultiObjectiveConstrainedHammingBallHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.MultiObjectiveConstrainedHammingBallHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.MultiObjectiveConstrainedHammingBallHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.ConstrainedVectorMovesSelector.Region;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.HillClimberListener.Reason;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.ConstrainedMNKLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.MNKLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.SingleThreadCPUTimer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class MultiObjectiveConstrainedHammingBallHillClimberExperiment implements Process {


	private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String PROBLEM_SEED_ARGUMENT = "pseed";
    private static final String MODEL_ARGUMENT = "model";
    private static final String Q_ARGUMENT = "q";
    private static final String K_ARGUMENT = "k";
    private static final String N_ARGUMENT = "n";
    private static final String SHIFT_ARGUMENT = "shift";
    private static final String D_ARGUMENT = "d";
    private static final String C_ARGUMENT = "c";
    
	private PrintStream ps;
	private ByteArrayOutputStream ba;
	private SingleThreadCPUTimer timer;
	
	private Random random;

    private Options options;
    
    ParetoNonDominatedSet nonDominatedSet;
    private int totalMoves;
    private int feasibleSolutionsGenerated;
    private int totalSolutionsGenerated;
    
	@Override
	public String getDescription() {
		return "Multi Objective Constrained Hamming Ball Hill Climber";
	}

    @Override
    public String getID() {
        return "moc-hbhc";
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
	    options.addOption(SHIFT_ARGUMENT, true, "shift in the values for the tables");
	    options.addOption(MODEL_ARGUMENT, true, "NK-model: y->adjacent, n->random, <number>->Localized");
	    options.addOption(PROBLEM_SEED_ARGUMENT, true, "random seed for generating the problem");
	    options.addOption(RADIUS_ARGUMENT, true, "radius of the Hamming Ball hill climber");
	    options.addOption(D_ARGUMENT, true, "number of objectives of the problem");
	    options.addOption(C_ARGUMENT, true, "constraints of the problem");
	    options.addOption(TIME_ARGUMENT, true, "execution time limit (in seconds)");
	    options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
	    
	    return options;
	}

	@Override
	public void execute(String[] args) {
	    
		if (args.length == 0) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(getID(), getOptions());
			return;
		}
		
		CommandLine commandLine = parseCommandLine(args);
		
		timer = new SingleThreadCPUTimer();
		timer.startTimer();
		
		initializeDataHolders();
		initializeOutput();
		
		final ConstrainedMNKLandscape pbf = configureProblem(commandLine);
		
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
        rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
        rballConfig.setProperty(RBallEfficientHillClimber.SEED, ""+seed);

        MultiObjectiveConstrainedHammingBallHillClimberForInstanceOf rballfio = 
                (MultiObjectiveConstrainedHammingBallHillClimberForInstanceOf) new MultiObjectiveConstrainedHammingBallHillClimber(rballConfig).initialize(pbf);
        
        ps.println("Search starts: "+timer.elapsedTimeInMilliseconds());
        

        while (!timer.shouldStop()) {
            double [] weights = generateRandomPositiveWeights(pbf.getDimension());
            PBSolution solution = getFeasibleInitialSolution(pbf);
            
            MultiObjectiveConstrainedHammingBallHillClimberSnapshot rball = rballfio.initialize(weights, solution);
            rball.setSeed(random.nextLong());
            
            rball.hillClimb(timer, new HillClimberListener() {
                @Override
                public void reportSolutionQuality(double[] solutionQuality) {
                    nonDominatedSet.reportSolutionToArchive(solutionQuality, pbf.getConstraintIndex());                    
                }
                
                @Override
                public void finished(Reason reason, long totalMoves) {
                    ps.println("Finished:"+reason+ " (moves: "+totalMoves+")");
                    MultiObjectiveConstrainedHammingBallHillClimberExperiment.this.totalMoves += totalMoves;
                }
                
                @Override
                public void enterNewRegion(Region region, long totalMoves) {
                    ps.println("Enter region:"+region+" (moves: "+totalMoves+")");
                }
            });
        }
        
        ps.println("Total random solutions generated:"+totalSolutionsGenerated);
        ps.println("Feasible random solutions generated: "+feasibleSolutionsGenerated);
        ps.println("Total moves: "+totalMoves);
        ps.println("Stored scores:" + rballfio.getStoredScores());
        ps.println("Total time (ms):"+timer.elapsedTimeInMilliseconds());
        ps.println("Average time per move (nanoseconds):"+timer.elapsedTime()/(double)totalMoves);
        ps.println(nonDominatedSet.printArchive());

        printOutput();
    }

    protected PBSolution getFeasibleInitialSolution(final ConstrainedMNKLandscape pbf) {
        boolean feasible = true;
        PBSolution sol;
        do {
            sol = pbf.getRandomSolution();
            totalSolutionsGenerated++;
            feasible = true;
            double [] quality = pbf.evaluate(sol);
            for (int i=pbf.getConstraintIndex(); i< quality.length; i++) {
                feasible &= (quality[i] >= 0); 
            }
            
        } while (!feasible);
        feasibleSolutionsGenerated++;
        return sol;
    }


    private ConstrainedMNKLandscape configureProblem(CommandLine commandLine) {
        String n = commandLine.getOptionValue(N_ARGUMENT);
        String k = commandLine.getOptionValue(K_ARGUMENT);
        String q = commandLine.getOptionValue(Q_ARGUMENT);
        String shift = commandLine.getOptionValue(SHIFT_ARGUMENT);
        int d = Integer.parseInt(commandLine.getOptionValue(D_ARGUMENT));
        int c = Integer.parseInt(commandLine.getOptionValue(C_ARGUMENT));
        String circular = commandLine.getOptionValue(MODEL_ARGUMENT);
        long problemSeed = Long.parseLong(commandLine.getOptionValue(PROBLEM_SEED_ARGUMENT));
        
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		prop.setProperty(MNKLandscape.DIMENSION_STRING, String.valueOf(d+c));
		prop.setProperty(ConstrainedMNKLandscape.CONSTRAINTS_STRING, String.valueOf(c));
		prop.setProperty(NKLandscapes.SHIFT_STRING, shift);

		if (!q.equals("-")) {
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}

		if (circular.equals("y")) {
			prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
		} else {
		    prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
		}
		
		ConstrainedMNKLandscape pbf = new ConstrainedMNKLandscape(problemSeed, prop);

		ps.println("N: " + n);
		ps.println("K: " + k);
		ps.println("Q: " + q);
		ps.println("D: " + d);
		ps.println("C: " + c);
		ps.println("Shift: " + shift);
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

}
