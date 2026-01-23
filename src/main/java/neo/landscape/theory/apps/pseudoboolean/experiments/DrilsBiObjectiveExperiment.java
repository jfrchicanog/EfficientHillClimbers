package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.*;
import neo.landscape.theory.apps.pseudoboolean.px.*;
import neo.landscape.theory.apps.util.*;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Timer;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

public class DrilsBiObjectiveExperiment implements Process {

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
    private static final String PROBLEM = "problem";
    private static final String CROSSOVER = "crossover";
    private static final String CROSSOVER_CHAR = "X";
    private static final String PROBLEM_CHAR = "P";
    private static final String TIMER_ARGUMENT = "timer";
    private static final String OBJECTIVE = "obj";

    private static final String MAXSAT_PROBLEM = "maxsat";
    private static final String NK_PROBLEM = "nk";
    private static final String NKSAT_PROBLEM = "nksat";
    private static final String ANK_RNK_PROBLEM = "ank_rnk";
    private static final String WALSH_PROBLEM = "walsh";

    private static final String DPX = "dpx";
    private static final String APX = "apx";
    private static final String PX = "px";
    private static final String NX = "nx";
    private static final String UX = "ux";
    private static final String SPX = "spx";
    private static final String FPX = "fpx";
    private static final String FDPX = "fdpx";
    private static final String CROSSOVER_NONE = "none";

    private static final String TYPE_PERTURBATION = "perturbation";
    private static final String TYPE_CROSSOVER = "crossover";
    private String filename = "output.gz";
    private BufferedWriter tsvWriter;

    private final Map<String, EmbeddedLandscapeConfigurator> configurators = new HashMap<>();
    private PBSolution bestSolution = null;

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
    private int numberOfExploredSolutions = 0;
    private int climbs = 0;

    private Options options;

    private Graph graph;
    private PBSolutionDigest solutionDigest;
    private Double localOptimumFitnessFilter;
    private CommandLine commandLine;
    private EmbeddedLandscapeConfigurator problemConfigurator;
    private String problem;
    private CrossoverConfigurator crossoverConfigurator;
    private String crossover;
    private String obj;
    private Predicate<?> shouldIStop;


    @Override
    public String getDescription() {
        return "Implementation of DRILS BiObjective";
    }

    @Override
    public String getID() {
        return "drilsbo";
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
        options.addOption(TIMER_ARGUMENT, true, "timer to use [" + Timers.SINGLE_THREAD_CPU + "," + Timers.CPU_CLOCK + "], default: " + Timers.getNameOfDefaultTimer());
        options.addOption(EXPLORED_SOLUTIONS, true, "explored solutions limit");
        options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
        options.addOption(LON_ARGUMENT, false, "print the PX Local Optima Network");
        options.addOption(LON_MINIMUM_FITNESS_ARGUMENT, true, "minimum fitness to consider a LON (optional)");
        options.addOption(DEBUG_ARGUMENT, false, "enable debug information");
        options.addOption(IMPROVING_LO, false, "accept only non disimproving local optima in ILS");
        options.addOption(PROBLEM, true, "problem to be solved: " + configurators.keySet());
        options.addOption(OBJECTIVE, true, "objective: obj1, obj2, obj1+obj2 " + configurators.keySet());
        options.addOption(CROSSOVER, true, "crossover operator to use: " + crossoverConf.keySet());
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
            obj = commandLine.getOptionValue(OBJECTIVE);
            boolean debug = commandLine.hasOption(DEBUG_ARGUMENT);

            EmbeddedLandscape pbf = getProblemConfigurator().configureProblem(commandLine.getOptionProperties(PROBLEM_CHAR), ps);

            RBallCrossover px = null;
            if (!crossover.equals(CROSSOVER_NONE)) {
                CrossoverInternal ci = getCrossoverConfigurator().configureCrossover(commandLine.getOptionProperties(CROSSOVER_CHAR), pbf, ps);
                px = new RBallCrossoverAdaptor(ci);
                px.setSeed(seed);
                px.setPrintStream(ps);
            }

            if (debug) {
                StringWriter sr = new StringWriter();
                if (pbf instanceof NKLandscapes) {
                    ((NKLandscapes) pbf).writeTo(sr);
                    ps.print(sr);
                }
                if (pbf instanceof BiObjectiveLandscape) {
                    ((BiObjectiveLandscape) pbf).writeTo(sr);
                    ps.print(sr);
                }
            }


            if (commandLine.hasOption(LON_ARGUMENT)) {
                initializeLONDataStructures(pbf);
            }

            if (!commandLine.hasOption(TIME_ARGUMENT) && !commandLine.hasOption(EXPLORED_SOLUTIONS)) {
                System.err.println("A stopping condition must be set using " + TIME_ARGUMENT + " or " + EXPLORED_SOLUTIONS);
                throw new IllegalArgumentException("A stopping condition must be set using " + TIME_ARGUMENT + " or " + EXPLORED_SOLUTIONS);
            }
            if (commandLine.hasOption(OUT_FILE)) {
                filename = commandLine.getOptionValue(OUT_FILE);
            }
            // user gives something like "output.gz"
            if (filename.endsWith(".gz")) {
                String tsvFile = filename.substring(0, filename.length() - 3) + ".tsv";
                tsvWriter = new BufferedWriter(new FileWriter(tsvFile, true));
                tsvWriter.write("alpha\tBiObjSol\tObj1\tObj2\tNorm_Obj1\tNorm_Obj2\tElapsedTime(ms)\tMoves\tBestSoFar\tOverallExecTime(ms)\tCrossovers\tdiffChild");
                tsvWriter.newLine();
                tsvWriter.flush();
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
            rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
            rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r + "");
            rballConfig.setProperty(RBallEfficientHillClimber.SEED, "" + seed);
            if (obj.equals("obj1+obj2")) {
                // center: alpha = 0.5
                algorithm(pbf, 0.5, rballConfig, perturbFactor, px);
                PBSolution bestSol = bestSolution;
                // go downward: 0.4, 0.3, 0.2, 0.1, 0.0
                for (int i = 4; i >= 0; i--) {
                    double alpha = i * 0.1;
                    algorithm(pbf, alpha, rballConfig, perturbFactor, px);
                }
                bestSolution = bestSol;

                // go upward: 0.6, 0.7, 0.8, 0.9, 1.0
                for (int i = 6; i <= 10; i++) {
                    double alpha = i * 0.1;
                    algorithm(pbf, alpha, rballConfig, perturbFactor, px);
                }
            } else if (obj.equals("small_increment")) {
                // center: alpha = 0.5
                algorithm(pbf, 0.5, rballConfig, perturbFactor, px);
                PBSolution bestSol = bestSolution;

                // go downward: 0.45, 0.40, 0.35, ..., 0.05, 0.00
                for (double i = 0.45; i >= 0.0; i -= 0.05) {
                    algorithm(pbf, i, rballConfig, perturbFactor, px);
                }

                bestSolution = bestSol;

                // go upward: 0.55, 0.60, 0.65, ..., 1.00
                for (double i = 0.55; i <= 1.0; i += 0.05) {
                    algorithm(pbf, i, rballConfig, perturbFactor, px);
                }
            } else if (obj.equals("smaller_increment")) {
                // center: alpha = 0.5
                algorithm(pbf, 0.5, rballConfig, perturbFactor, px);
                PBSolution bestSol = bestSolution;

                /// go downward: 0.49, 0.48, 0.47, ..., 0.01, 0.00
                for (double i = 0.49; i >= 0.0; i -= 0.01) { // Starting from 0.49 and step size 0.01
                    algorithm(pbf, i, rballConfig, perturbFactor, px);
                }

                bestSolution = bestSol;

                // go upward: 0.51, 0.52, 0.53, ..., 1.00
                for (double i = 0.51; i <= 1.0; i += 0.01) { // Starting from 0.51 and step size 0.01
                    algorithm(pbf, i, rballConfig, perturbFactor, px);
                }
            } else if (obj.equals("obj2")) {
                // start at 0.03, 10 iterations increasing 0.1
                // α = 0.03 + k*0.1  for k = 0..9
                for (int k = 0; k < 10; k++) {
                    double alpha = 0.03 + k * 0.1;
                    algorithm(pbf, alpha, rballConfig, perturbFactor, px);
                }
            } else if (obj.equals("obj1")) {
                // start at 0.97, 10 iterations decreasing 0.1
                // α = 0.97 - k*0.1  for k = 0..9
                for (int k = 0; k < 10; k++) {
                    double alpha = 0.97 - k * 0.1;
                    algorithm(pbf, alpha, rballConfig, perturbFactor, px);
                }
            }
            ps.println("Overall Execution Time(milli seconds): " + timer.elapsedTimeInMilliseconds());
            printOutput();
        } catch (RuntimeException e) {
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            showOptions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void algorithm(EmbeddedLandscape pbf, double factor, Properties rballConfig, double perturbFactor, RBallCrossover px) throws IOException {
        int countChild = 0;
        int countCrossover = 0;
        numberOfExploredSolutions = 0;
        pbf.setAlpha(factor);
        RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(rballConfig).initialize(pbf);
        long executedTime = timer.elapsedTimeInMilliseconds();

        shouldIStop = (x -> false);
        if (commandLine.hasOption(TIME_ARGUMENT)) {
            long timeInMilliSeconds = 1000L * Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
            timer.setStopTimeMilliseconds(timer.elapsedTimeInMilliseconds() + timeInMilliSeconds);
            shouldIStop = shouldIStop.or(x -> timer.shouldStop());
        }
        if (commandLine.hasOption(EXPLORED_SOLUTIONS)) {
            int expSols = Integer.parseInt(commandLine.getOptionValue(EXPLORED_SOLUTIONS));
//            final int maxExploredSolutions = Double.compare(factor, 0.5) == 0 ? expSols : (int) (expSols*(1- (Math.abs(0.5 - factor))));
//            final int maxExploredSolutions = (int) (500*(1.0 + factor));
            final int maxExploredSolutions = expSols;
            shouldIStop = shouldIStop.or(x -> numberOfExploredSolutions >= maxExploredSolutions);
//                    shouldIStop = shouldIStop.or(x -> climbs >= maxExploredSolutions);
            shouldIStop = shouldIStop.or(x -> climbs >= pbf.getN());
        }
        ps.println("-----------------------------------Alpha=" + factor + "-----------------------------------");
        try {
            RBallEfficientHillClimberSnapshot previousSolution = (bestSolution == null) ? createGenerationZeroSolution(rballfio) : createGenerationSolution(rballfio, bestSolution);
            notifyExploredSolution(previousSolution);
            int perturbMoves = 20;
            while (!shouldIStop.test(null)) {
                RBallEfficientHillClimberSnapshot currentSolution = rballfio.initialize(new PBSolution(previousSolution.getSolution()), previousSolution);
                if (perturbFactor < 0) {
                    if (moves > 0) {
                        perturbMoves = moves;
                    }
                } else {
                    perturbMoves = (int) (perturbFactor * pbf.getN());
                }

                currentSolution.softRestart(perturbMoves);
                ps.println("* Hamming distance after perturbation: " + currentSolution.getSolution().hammingDistance(previousSolution.getSolution()));
                hillClimb(currentSolution);
                notifyExploredSolution(currentSolution);
                reportLONEdge(previousSolution, currentSolution, TYPE_PERTURBATION);

                RBallEfficientHillClimberSnapshot child = null;
                if (px != null && !shouldIStop.test(null)) {
                    child = px.recombine(previousSolution, currentSolution);
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
                }
                previousSolution = acceptanceCriterion(previousSolution, child);
            }
        } catch (Exception e) {
            ps.println("Exception: " + e.getMessage());
            e.printStackTrace(ps);
        }
        writeLONInformation();

        executedTime = timer.elapsedTimeInMilliseconds() - executedTime;
        ps.println("Execution Time: " + executedTime);
        ps.println("No. of crossovers Operated: " + countCrossover);
        ps.println("No. of children different from parents: " + countChild);
        tsvWriter.write(
                "0\t0\t0\t0\t0\t0\t0\t0\tEND\t" + executedTime + "\t" + countCrossover + "\t" + countChild
        );
        tsvWriter.newLine();
        tsvWriter.flush();
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
            helpFormatter.printHelp("Problem: " + problem, problemOptions);
        } catch (RuntimeException e) {
        }

        try {
            Options crossoverOptions = new Options();
            getCrossoverConfigurator().prepareOptionsForCrossover(crossoverOptions);
            helpFormatter.printHelp("Crossover: " + crossover, crossoverOptions);
        } catch (RuntimeException e) {
        }
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
            throw new RuntimeException(e);
        }
    }

    private void initializeStatistics() {
        bestSoFar = -Double.MAX_VALUE;
    }

    private void initializeOutput() {
        ba = new ByteArrayOutputStream();
        // Create a dummy PrintStream that discards all output
        ps = new PrintStream(new java.io.OutputStream() {
            @Override
            public void write(int b) {
                // Discard output to save memory
            }
        });
//        try {
//            ps = new PrintStream(new GZIPOutputStream(ba));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    private void printOutput() {
        // Do nothing.
        // The TSV file has already been written to disk incrementally.
        /*try {
            System.out.write(ba.toByteArray());
            writeCompressedToFile(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
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
        double obj1 = Double.NaN;
        double obj2 = Double.NaN;
        boolean isBest = Boolean.FALSE;
        double alpha = exploredSolution.getProblem().getAlpha();
        double quality = exploredSolution.getSolutionQuality();
        numberOfExploredSolutions++;

        ps.println("Solution: " + exploredSolution.getSolution().toString());
        ps.println("Solution quality: " + quality);

        if (problem.equals(ANK_RNK_PROBLEM)) {
            // In this problem type, first problem is NK and second one is RNK
            BiObjectiveLandscape problem = (BiObjectiveLandscape) exploredSolution.getProblem();
//            List<Integer> nValues = problem.getNValues();
            List<Integer> mValues = problem.getMValues();

            Double[] subfnsEvals = exploredSolution.getSubfnsEvals();

            obj1 = Arrays.stream(subfnsEvals)
                    .limit(mValues.get(0))
                    .mapToDouble(Double::doubleValue)
                    .sum();
            ps.println("** ANK Solution quality: " + obj1);

            obj2 = quality - obj1;
            ps.println("** RNK Solution quality: " + obj2);
        }
        long timeElapsed = timer.elapsedTimeInMilliseconds();
        ps.println("Elapsed Time: " + timeElapsed);
        ps.println("* Moves: " + moves);
        if (quality > bestSoFar) {
            isBest = Boolean.TRUE;
            bestSoFar = quality;
            bestSolution = exploredSolution.getSolution();
            ps.println("* Best so far solution");
        }

//        tsvWriter.write("alpha\tBiObjSol\tObj1\tObj2\tNorm_Obj1\tNorm_Obj2\tElapsedTime(ms)\tMoves\tBestSoFar\tOverallExecTime(ms)");
        try {
            // ----- NORMALIZATION -----
            double normObj1 = obj1;
            double normObj2 = obj2;

            if (alpha > 0.0) {
                normObj1 = obj1 / alpha;
            }

            if (alpha < 1.0) {
                normObj2 = obj2 / (1.0 - alpha);
            }
            tsvWriter.write(
                    alpha + "\t" +
                            quality + "\t" +
                            obj1 + "\t" +
                            obj2 + "\t" +
                            normObj1 + "\t" +
                            normObj2 + "\t" +
                            timeElapsed + "\t" +
                            moves + "\t" +
                            (isBest ? "YES" : "NO") + "\t" +
                            0 + "\t" +
                            0 + "\t" +
                            0
            );
            tsvWriter.newLine();
            tsvWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private RBallEfficientHillClimberSnapshot createGenerationSolution(RBallEfficientHillClimberForInstanceOf rballfio, PBSolution pbs) {
        RBallEfficientHillClimberSnapshot rball = rballfio.initialize(pbs);
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
            if (localOptimumFitnessFilter == null ||
                    (solution.getSolutionQuality() >= localOptimumFitnessFilter &&
                            result.getSolutionQuality() >= localOptimumFitnessFilter)) {
                graph.addEdge(solutionDigest.getHashOfSolution(solution),
                        solutionDigest.getHashOfSolution(result), kind);
            }
        }
    }

    private void reportLONNode(RBallEfficientHillClimberSnapshot solution) {
        if (graph != null) {
            if (localOptimumFitnessFilter == null || solution.getSolutionQuality() >= localOptimumFitnessFilter) {
                graph.addNode(solutionDigest.getHashOfSolution(solution), solution.getSolutionQuality());
            }
        }
    }

    private void writeLONInformation() {
        if (graph != null) {
            ps.println("LON nodes:" + graph.printNodes());
            ps.println("LON edges:" + graph.printEdges());
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
        if (problemConfigurator == null) {
            problemConfigurator = createEmbeddedLandscapeConfigurator();
        }
        return problemConfigurator;
    }

    protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
        EmbeddedLandscapeConfigurator elc = configurators.get(problem);
        if (elc == null) {
            throw new IllegalArgumentException("Problem " + problem + " is unknown");
        }
        return elc;
    }

    protected CrossoverConfigurator createCrossoverConfigurator() {
        CrossoverConfigurator xConf = crossoverConf.get(crossover);
        if (xConf == null) {
            throw new IllegalArgumentException("Crossover " + crossover + " is unknown");
        }
        return xConf;
    }

    private CrossoverConfigurator getCrossoverConfigurator() {
        if (crossoverConfigurator == null) {
            crossoverConfigurator = createCrossoverConfigurator();
        }
        return crossoverConfigurator;
    }


}
