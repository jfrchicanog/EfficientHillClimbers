package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.AnkRnkLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.BiObjectiveLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.px.ParetoPartitionCrossover;
import neo.landscape.theory.apps.pseudoboolean.px.RBallCrossover;
import neo.landscape.theory.apps.pseudoboolean.px.RBallCrossoverAdaptor;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.Timer;
import neo.landscape.theory.apps.util.Timers;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Predicate;

public class McDrilsMoBiObjExperiment implements Process {

    // --- Arguments ---
    private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
    private static final String TIME_ARGUMENT = "time";
    private static final String OUT_FILE = "outfile";
    private static final String EXPLORED_SOLUTIONS = "expSols";
    private static final String MOVES_FACTOR_ARGUMENT = "mf";
    private static final String RADIUS_ARGUMENT = "r";
    private static final String PROBLEM_CHAR = "P";
    private static final String DEBUG_ARGUMENT = "debug";

    // --- Fields ---
    private PrintStream ps;
    private Timer timer;
    private long seed;

    private double bestSoFar = -Double.MAX_VALUE;
    private PBSolution bestSolution = null;
    private int moves = 0;
    private int numberOfExploredSolutions = 0;
    private int climbs = 0;

    private BufferedWriter tsvWriter;
    private Options options;
    private CommandLine commandLine;
    private Predicate<?> shouldIStop;
    private ParetoNonDominatedSet archive;

    @Override
    public String getDescription() {
        return "Drils Pareto (Center-Out Sweep + Neighbors Crossover - Optimized I/O)";
    }

    @Override
    public String getID() {
        return "drils-pareto";
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
            options.addOption(TIME_ARGUMENT, true, "Time limit");
            options.addOption(OUT_FILE, true, "Output file");
            options.addOption(EXPLORED_SOLUTIONS, true, "Solutions limit (per alpha step)");
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
            System.out.println("Algorithm: DRILS Pareto (Center-Out Sweep + Harvesting)");

            // 1. Configure Problem
            AnkRnkLandscapeConfigurator configurator = new AnkRnkLandscapeConfigurator();
            EmbeddedLandscape pbf = configurator.configureProblem(commandLine.getOptionProperties(PROBLEM_CHAR), ps);
            if (!(pbf instanceof BiObjectiveLandscape))
                throw new IllegalArgumentException("Requires BiObjectiveLandscape");
            BiObjectiveLandscape biObjPbf = (BiObjectiveLandscape) pbf;

            // 2. Setup Archive & Crossover
            archive = new ParetoNonDominatedSet();
            ParetoPartitionCrossover paretoPx = new ParetoPartitionCrossover(biObjPbf);
            paretoPx.setArchive(archive);
            paretoPx.setSeed(seed);
            paretoPx.setPrintStream(ps);
            RBallCrossover px = new RBallCrossoverAdaptor(paretoPx);

            // 3. Setup Trajectory Logging (Only if -debug is passed)
            if (commandLine.hasOption(OUT_FILE) && commandLine.hasOption(DEBUG_ARGUMENT)) {
                String filename = commandLine.getOptionValue(OUT_FILE);
                String debugFilename;

                if (filename.endsWith(".tsv")) {
                    debugFilename = filename.substring(0, filename.length() - 4) + "_debug.tsv";
                } else {
                    debugFilename = filename + "_debug.tsv";
                }

                tsvWriter = new BufferedWriter(new FileWriter(debugFilename, false));
                tsvWriter.write("alpha\tBiObjSol\tRawObj1\tRawObj2\tElapsedTime(ms)\tMoves");
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
            // CENTER-OUT SWEEP LOGIC (0.05 Increments)
            // ===============================================================

            // Phase 1: Center (0.5)
            System.out.println("--- Phase 1: Center (0.5) ---");
            RBallEfficientHillClimberSnapshot anchor = runSingleAlphaStep(biObjPbf, 0.5, null, rballConfig, perturbFactor, px);

            // Phase 2: Sweep Down (Towards Obj2)
            System.out.println("--- Phase 2: Sweep Down (Towards Obj2) ---");
            RBallEfficientHillClimberSnapshot current = anchor;
            double[] downSteps = {0.45, 0.40, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05, 0.0};
            for (double a : downSteps) {
                current = runSingleAlphaStep(biObjPbf, a, current, rballConfig, perturbFactor, px);
            }

            // Phase 3: Sweep Up (Towards Obj1)
            System.out.println("--- Phase 3: Sweep Up (Towards Obj1) ---");
            current = anchor; // Reset to center anchor
            double[] upSteps = {0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0};
            for (double a : upSteps) {
                current = runSingleAlphaStep(biObjPbf, a, current, rballConfig, perturbFactor, px);
            }

            // Clean up debug writer
            if (tsvWriter != null) {
                tsvWriter.close();
            }

            // ==========================================================
            // 5. SAVE THE PURE ARCHIVE TO THE MAIN OUTPUT FILE
            // ==========================================================
            long totalExecutionTime = timer.elapsedTimeInMilliseconds();
            System.out.println("Overall Execution Time(ms): " + totalExecutionTime);

            if (commandLine.hasOption(OUT_FILE)) {
                String filename = commandLine.getOptionValue(OUT_FILE);
                saveArchiveToFile(archive, filename, totalExecutionTime);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs ILS for a single fixed alpha.
     */
    private RBallEfficientHillClimberSnapshot runSingleAlphaStep(
            BiObjectiveLandscape pbf,
            double alpha,
            RBallEfficientHillClimberSnapshot startSolution,
            Properties rballConfig,
            double perturbFactor,
            RBallCrossover px) throws IOException {

        pbf.setAlpha(alpha);

        RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf)
                new RBallEfficientHillClimber(rballConfig).initialize(pbf);

        numberOfExploredSolutions = 0;
        setupStoppingCondition(pbf.getN());

        RBallEfficientHillClimberSnapshot previousSolution;
        if (startSolution == null) {
            previousSolution = createSolution(rballfio, null);
        } else {
            previousSolution = rballfio.initialize(startSolution.getSolution());
            hillClimb(previousSolution);
        }

        notifyExploredSolution(previousSolution);

        int perturbMoves = (int) (perturbFactor * pbf.getN());

        try {
            while (!shouldIStop.test(null)) {

                RBallEfficientHillClimberSnapshot currentSolution = rballfio.initialize(
                        new PBSolution(previousSolution.getSolution()), previousSolution);

                currentSolution.softRestart(perturbMoves);
                hillClimb(currentSolution);
                notifyExploredSolution(currentSolution);

                RBallEfficientHillClimberSnapshot child = px.recombine(previousSolution, currentSolution);

                if (child != null) {
                    hillClimb(child);
                    notifyExploredSolution(child);
                    previousSolution = child;
                } else {
                    previousSolution = currentSolution;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(ps);
        }

        return previousSolution;
    }

    private void hillClimb(RBallEfficientHillClimberSnapshot rball) {
        moves = 0;
        climbs = 0;
        int maxClimbs = rball.getProblem().getN(); // Get N dynamically
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

    private RBallEfficientHillClimberSnapshot createSolution(RBallEfficientHillClimberForInstanceOf rballfio, PBSolution start) {
        PBSolution init = (start == null) ? rballfio.getProblem().getRandomSolution() : start;
        RBallEfficientHillClimberSnapshot rball = rballfio.initialize(init);
        rball.setSeed(seed);
        hillClimb(rball);
        return rball;
    }

    private void setupStoppingCondition(int N) {
        shouldIStop = (x -> false);

        if (commandLine.hasOption(TIME_ARGUMENT)) {
            long limit = 1000L * Integer.parseInt(commandLine.getOptionValue(TIME_ARGUMENT));
            timer.setStopTimeMilliseconds(timer.elapsedTimeInMilliseconds() + limit);
            shouldIStop = shouldIStop.or(x -> timer.shouldStop());
        }

        if (commandLine.hasOption(EXPLORED_SOLUTIONS)) {
            int limit = Integer.parseInt(commandLine.getOptionValue(EXPLORED_SOLUTIONS));
            shouldIStop = shouldIStop.or(x -> numberOfExploredSolutions >= limit);
        }
    }

    private void notifyExploredSolution(RBallEfficientHillClimberSnapshot exploredSolution) {
        numberOfExploredSolutions++;
        double quality = exploredSolution.getSolutionQuality();
        double currentAlpha = exploredSolution.getProblem().getAlpha();

        BiObjectiveLandscape problem = (BiObjectiveLandscape) exploredSolution.getProblem();
        List<Integer> mValues = problem.getMValues();
        Double[] subfnsEvals = exploredSolution.getSubfnsEvals();

        double weightedObj1 = Arrays.stream(subfnsEvals).limit(mValues.get(0)).mapToDouble(Double::doubleValue).sum();
        double weightedObj2 = Arrays.stream(subfnsEvals).skip(mValues.get(0)).mapToDouble(Double::doubleValue).sum();

        double rawObj1 = (currentAlpha > 1e-9) ? weightedObj1 / currentAlpha : 0.0;
        double rawObj2 = ((1 - currentAlpha) > 1e-9) ? weightedObj2 / (1 - currentAlpha) : 0.0;

        // Catch ALL evaluated points in the global archive
        archive.reportSolutionToArchive(new double[]{rawObj1, rawObj2});

        try {
            if (tsvWriter != null) {
                tsvWriter.write(String.format(Locale.US, "%.4f\t%.2f\t%.2f\t%.2f\t%d\t%d",
                        currentAlpha, quality, rawObj1, rawObj2, timer.elapsedTimeInMilliseconds(), moves));
                tsvWriter.newLine();
                tsvWriter.flush();
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

    private void saveArchiveToFile(ParetoNonDominatedSet targetArchive, String outputFilename, long executionTime) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename, false));

            // Embed the execution time natively as a comment at the top of the file
            writer.write("# Total Execution Time (ms): " + executionTime + "\n");
            writer.write("Norm_Obj1\tNorm_Obj2\n");

            String[] lines = targetArchive.printArchive().split("\n");

            for (String line : lines) {
                if (line.trim().isEmpty() || line.toLowerCase().contains("archive") || line.toLowerCase().contains("points")) {
                    continue;
                }

                String[] parts = line.replace(",", " ").trim().split("\\s+");

                if (parts.length >= 2) {
                    try {
                        double rawObj1 = Double.parseDouble(parts[0]);
                        double rawObj2 = Double.parseDouble(parts[1]);
                        String cleanLine = String.format(Locale.US, "%.2f\t%.2f", rawObj1, rawObj2);
                        writer.write(cleanLine + "\n");
                    } catch (NumberFormatException e) {
                        String cleanLine = line.replace(",", "").trim().replaceAll("\\s+", "\t");
                        writer.write(cleanLine + "\n");
                    }
                }
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