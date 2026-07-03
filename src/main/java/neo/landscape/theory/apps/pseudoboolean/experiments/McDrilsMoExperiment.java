package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MultiObjLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.MultiObjectiveLandscape;
import neo.landscape.theory.apps.pseudoboolean.px.QPartitionCrossover;
import neo.landscape.theory.apps.pseudoboolean.px.RBallCrossover;
import neo.landscape.theory.apps.pseudoboolean.px.RBallCrossoverAdaptor;
import neo.landscape.theory.apps.pseudoboolean.util.NonDominatedSet;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.Timer;
import neo.landscape.theory.apps.util.Timers;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

public class McDrilsMoExperiment implements Process {

    // --- Arguments ---
    private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String OUT_FILE = "outfile";
    private static final String EXPLORED_SOLUTIONS = "expSols";
    private static final String MOVES_FACTOR_ARGUMENT = "mf";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String PROBLEM_CHAR = "P";
    private static final String STEP_SIZE = "step_size";
    private static final String DEBUG_ARGUMENT = "debug";

    // --- Fields ---
    private PrintStream ps;
    private Timer timer;
    private long seed;

    private int moves = 0;
    private int numberOfExploredSolutions = 0;

    private BufferedWriter tsvWriter;
    private Options options;
    private CommandLine commandLine;
    private Predicate<?> shouldIStop;
    private NonDominatedSet archive;
    private int numObjectives;

    // Zero-Allocation Class Buffer for high-frequency logging
    private final StringBuilder logLineBuffer = new StringBuilder(256);

    @Override
    public String getDescription() {
        return "McDRILS-MO N-Objective Pareto (Recursive Center-Out Simplex Sweep + Neighbors Crossover)";
    }

    @Override
    public String getID() {
        return "mcdrilsmo";
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
            options = new Options();
            options.addOption(RADIUS_ARGUMENT, true, "Radius (r)");
            options.addOption(MOVES_FACTOR_ARGUMENT, true, "Mutation Factor (mf)");
            options.addOption(TIME_ARGUMENT, true, "Time limit (per weight step)");
            options.addOption(OUT_FILE, true, "Output file");
            options.addOption(EXPLORED_SOLUTIONS, true, "Solutions limit (per weight step)");
            options.addOption(ALGORITHM_SEED_ARGUMENT, true, "Seed");
            options.addOption(DEBUG_ARGUMENT, false, "Enable full trajectory logging");
            options.addOption(Option.builder(PROBLEM_CHAR).numberOfArgs(2).valueSeparator().argName("P=V").desc("Properties").build());
        }
        return options;
    }

    @Override
    public void execute(String[] args) {
        try {
            commandLine = new DefaultParser().parse(getOptions(), args);
            configureTimer();
            timer.startTimer();
            initializeOutput();

            seed = commandLine.hasOption(ALGORITHM_SEED_ARGUMENT) ?
                    Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT)) : Seeds.getSeed();

            System.out.println("Seed: " + seed);
            System.out.println("Algorithm: McDRILSMo N-Objective Pareto (Center-Out Sweep + Harvesting)");

            // 1. Configure Problem
            MultiObjLandscapeConfigurator configurator = new MultiObjLandscapeConfigurator();
            Properties pbmProps = commandLine.getOptionProperties(PROBLEM_CHAR);

            EmbeddedLandscape pbf = configurator.configureProblem(pbmProps, ps);
            if (!(pbf instanceof MultiObjectiveLandscape))
                throw new IllegalArgumentException("Requires MultiObjectiveLandscape");
            MultiObjectiveLandscape multiObjPbf = (MultiObjectiveLandscape) pbf;
            this.numObjectives = multiObjPbf.getNumObjectives();

            // 2. Setup Archive & Crossover
            archive = new NonDominatedSet(pbf.getN());
            QPartitionCrossover paretoPx = new QPartitionCrossover(multiObjPbf);
            paretoPx.setArchive(archive);
            paretoPx.setSeed(seed);
            paretoPx.setPrintStream(ps);
            RBallCrossover px = new RBallCrossoverAdaptor(paretoPx);

            // 3. Setup Trajectory Logging
            if (commandLine.hasOption(OUT_FILE) && commandLine.hasOption(DEBUG_ARGUMENT)) {
                String filename = commandLine.getOptionValue(OUT_FILE);
                String debugFilename = filename.endsWith(".tsv") ?
                        filename.substring(0, filename.length() - 4) + "_debug.tsv" : filename + "_debug.tsv";

                tsvWriter = new BufferedWriter(new FileWriter(debugFilename, false));

                // Dynamically build TSV Header
                StringBuilder header = new StringBuilder();
                for (int i = 1; i <= numObjectives; i++) header.append("L").append(i).append("\t");
                header.append("SolQuality\t");
                for (int i = 1; i <= numObjectives; i++) header.append("RawObj").append(i).append("\t");
                header.append("ElapsedTime(ms)\tMoves");

                tsvWriter.write(header.toString());
                tsvWriter.newLine();
                tsvWriter.flush();
            }

            // 4. Configuration
            Properties rballConfig = new Properties();
            rballConfig.setProperty(RBallEfficientHillClimber.RANDOM_MOVES, "yes");
            rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, commandLine.getOptionValue(RADIUS_ARGUMENT));
            rballConfig.setProperty(RBallEfficientHillClimber.SEED, "" + seed);
            double perturbFactor = Double.parseDouble(commandLine.getOptionValue(MOVES_FACTOR_ARGUMENT));

            // ===============================================================
            // N-DIMENSIONAL CENTER-OUT SIMPLEX SWEEP LOGIC
            // ===============================================================
            double stepSize = Double.parseDouble(pbmProps.getProperty(STEP_SIZE,"0.5"));
            List<double[]> simplexGrid = generateCenterOutSimplexGrid(numObjectives, stepSize);

            System.out.println("--- Starting " + numObjectives + "D Simplex Sweep (" + simplexGrid.size() + " targets) ---");

            // Start with a null PBSolution so the first run initializes randomly
            PBSolution currentSeed = null;

            for (int i = 0; i < simplexGrid.size(); i++) {
                double[] w = simplexGrid.get(i);
                System.out.printf("Evaluating Target %d/%d: %s\n", i + 1, simplexGrid.size(), Arrays.toString(w));
                currentSeed = runWeightedDrils(multiObjPbf, w, currentSeed, rballConfig, perturbFactor, px);
            }

            if (tsvWriter != null) {
                tsvWriter.close();
            }

            // ==========================================================
            // 5. SAVE THE PURE ARCHIVE
            // ==========================================================
            long totalExecutionTime = timer.elapsedTimeInMilliseconds();
            System.out.println("Overall Execution Time(ms): " + totalExecutionTime);

            if (commandLine.hasOption(OUT_FILE)) {
                String filename = commandLine.getOptionValue(OUT_FILE);
                saveArchiveToFile(archive, filename, totalExecutionTime, numObjectives);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dynamically generates an N-dimensional simplex grid and sorts it radially from the centroid.
     */
    private List<double[]> generateCenterOutSimplexGrid(int numObjs, double step) {
        List<double[]> grid = new ArrayList<>();
        double[] initialWeights = new double[numObjs];

        // Populate the grid recursively
        generateSimplexRecursive(grid, initialWeights, 0, 1.0, step);

        // Sort geographically outward from centroid
        double[] centroid = new double[numObjs];
        Arrays.fill(centroid, 1.0 / numObjs);

        grid.sort(Comparator.comparingDouble(w -> {
            double distSq = 0;
            for (int i = 0; i < numObjs; i++) {
                distSq += Math.pow(w[i] - centroid[i], 2);
            }
            return distSq;
        }));

        return grid;
    }

    /**
     * Recursive helper to build the N-dimensional simplex grid where sum(weights) = 1.0
     */
    private void generateSimplexRecursive(List<double[]> grid, double[] currentWeights, int depth, double remaining, double step) {
        int numObjs = currentWeights.length;

        // Base case: we are at the final objective dimension
        if (depth == numObjs - 1) {
            currentWeights[depth] = Math.max(0.0, Math.round(remaining * 100.0) / 100.0);
            grid.add(currentWeights.clone());
            return;
        }

        // Recursive case: iterate through possible values for this dimension
        for (double val = 0.0; val <= remaining + 1e-9; val += step) {
            double roundedVal = Math.round(val * 100.0) / 100.0;
            currentWeights[depth] = roundedVal;
            generateSimplexRecursive(grid, currentWeights, depth + 1, remaining - roundedVal, step);
        }
    }

    /**
     * Configures the stopping condition locally.
     * Applies the PER-WEIGHT time limit and solution evaluations limits.
     */
    private void setupStoppingCondition() {
        shouldIStop = (x -> false);
        if (commandLine.hasOption(TIME_ARGUMENT)) {
            long limit = 1000L * Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
            // Recalculate deadline relative to the exact moment this weight vector starts
            timer.setStopTimeMilliseconds(timer.elapsedTimeInMilliseconds() + limit);
            shouldIStop = shouldIStop.or(x -> timer.shouldStop());
        }
        if (commandLine.hasOption(EXPLORED_SOLUTIONS)) {
            int limit = Integer.parseInt(commandLine.getOptionValue(EXPLORED_SOLUTIONS));
            shouldIStop = shouldIStop.or(x -> numberOfExploredSolutions >= limit);
        }
    }

    private PBSolution runWeightedDrils(
            MultiObjectiveLandscape pbf,
            double[] weights,
            PBSolution startSolution,
            Properties rballConfig,
            double perturbFactor,
            RBallCrossover px) {

        pbf.setWeights(weights);

        RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf)
                new RBallEfficientHillClimber(rballConfig).initialize(pbf);

        // Reset the budget locally for THIS specific weight
        numberOfExploredSolutions = 0;
        setupStoppingCondition();

        RBallEfficientHillClimberSnapshot previousSolution;

        if (startSolution == null) {
            previousSolution = createRandomSolution(rballfio);
        } else {
            previousSolution = rballfio.initialize(new PBSolution(startSolution));
            hillClimb(previousSolution);
        }

        notifyExploredSolution(previousSolution);

        // Track the absolute best solution found in THIS specific run
        PBSolution bestOfRun = new PBSolution(previousSolution.getSolution());
        double bestQualityOfRun = previousSolution.getSolutionQuality();

        int perturbMoves = (int) (perturbFactor * pbf.getN());

        try {
            while (!shouldIStop.test(null)) {

                RBallEfficientHillClimberSnapshot currentSolution = rballfio.initialize(
                        new PBSolution(previousSolution.getSolution()), previousSolution);

                currentSolution.softRestart(perturbMoves);
                hillClimb(currentSolution);
                notifyExploredSolution(currentSolution);

                // Update tracker if the mutation reached a new peak
                if (currentSolution.getSolutionQuality() > bestQualityOfRun) {
                    bestQualityOfRun = currentSolution.getSolutionQuality();
                    bestOfRun = new PBSolution(currentSolution.getSolution());
                }

                RBallEfficientHillClimberSnapshot child = px.recombine(previousSolution, currentSolution);

                if (child != null) {
                    hillClimb(child);
                    notifyExploredSolution(child);

                    // Update tracker if crossover reached a new peak
                    if (child.getSolutionQuality() > bestQualityOfRun) {
                        bestQualityOfRun = child.getSolutionQuality();
                        bestOfRun = new PBSolution(child.getSolution());
                    }

                    previousSolution = child;
                } else {
                    previousSolution = currentSolution;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(ps);
        }

        // Return the absolute best found, NOT the last evaluated node of the random walk
        return bestOfRun;
    }

    private void hillClimb(RBallEfficientHillClimberSnapshot rball) {
        moves = 0; // Local reset per hill climb execution to track phase depth
        int climbs = 0;
        int maxClimbs = rball.getProblem().getN();
        try {
            do {
                rball.move();
                moves++;
                climbs++;
            } while (!shouldIStop.test(null) && climbs < maxClimbs);
        } catch (NoImprovingMoveException e) {
            // Local optimum reached, exception safely caught
        }
    }

    private RBallEfficientHillClimberSnapshot createRandomSolution(RBallEfficientHillClimberForInstanceOf rballfio) {
        PBSolution randomInit = rballfio.getProblem().getRandomSolution();

        RBallEfficientHillClimberSnapshot rball = rballfio.initialize(randomInit);
        rball.setSeed(seed);
        hillClimb(rball);

        return rball;
    }

    private void notifyExploredSolution(RBallEfficientHillClimberSnapshot exploredSolution) {
        numberOfExploredSolutions++;
        double quality = exploredSolution.getSolutionQuality();

        MultiObjectiveLandscape problem = (MultiObjectiveLandscape) exploredSolution.getProblem();
        double[] currentWeights = problem.getWeights();
        List<Integer> mValues = problem.getMValues();

        // Using the standard framework method
        Double[] subfnsEvals = exploredSolution.getSubfnsEvals();

        double[] rawObjs = new double[numObjectives];
        int skip = 0;

        // Dynamically sum the evaluations for N distinct objectives
        for (int i = 0; i < numObjectives; i++) {
            double weightedObj = 0;
            int limit = skip + mValues.get(i);

            // Raw for-loop replacing the Stream API
            for (int j = skip; j < limit; j++) {
                weightedObj += subfnsEvals[j];
            }

            rawObjs[i] = (currentWeights[i] > 1e-9) ? weightedObj / currentWeights[i] : 0.0;
            skip += mValues.get(i);
        }

        archive.reportSolutionToArchive(rawObjs);

        try {
            if (tsvWriter != null) {
                // Instantly wipe the buffer clean instead of allocating a new StringBuilder
                logLineBuffer.setLength(0);

                for (int i = 0; i < numObjectives; i++) {
                    logLineBuffer.append(Math.round(currentWeights[i] * 100.0) / 100.0).append("\t");
                }

                logLineBuffer.append(Math.round(quality * 100.0) / 100.0).append("\t");

                for (int i = 0; i < numObjectives; i++) {
                    logLineBuffer.append(Math.round(rawObjs[i] * 100.0) / 100.0).append("\t");
                }

                logLineBuffer.append(timer.elapsedTimeInMilliseconds()).append("\t").append(moves);

                tsvWriter.write(logLineBuffer.toString());
                tsvWriter.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureTimer() {
        timer = Timers.getDefaultTimer();
    }

    private void initializeOutput() {
        ps = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
    }

    private void saveArchiveToFile(NonDominatedSet targetArchive, String outputFilename, long executionTime, int nObjs) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename, false));

            writer.write("# Total Execution Time (ms): " + executionTime + "\n");

            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= nObjs; i++) {
                header.append("Norm_Obj").append(i).append(i == nObjs ? "" : "\t");
            }
            writer.write(header + "\n");

            // Streams cleanly from the List without splitting massive strings in memory
            List<double[]> solutions = targetArchive.getSolutions();

            for (double[] rawObjs : solutions) {
                StringBuilder cleanLine = new StringBuilder();
                for (int i = 0; i < nObjs; i++) {
                    cleanLine.append(String.format(Locale.US, "%.2f", rawObjs[i])).append(i == nObjs - 1 ? "" : "\t");
                }
                writer.write(cleanLine + "\n");
            }

            writer.flush();
            writer.close();
            System.out.println("Successfully saved pure Pareto front to: " + outputFilename);

        } catch (IOException e) {
            System.err.println("Error writing archive to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}