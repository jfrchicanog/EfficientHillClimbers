package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.MultiObjectiveLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.NonDominatedSet;

import java.util.Arrays;
import java.util.List;

/**
 * <b>Q Partition Crossover (QPX) using Best + Neighbors sampling</b>
 * * Scaled to support an arbitrary number of objectives.
 * * Hyper-optimized for Zero-Allocation in the hot loops (N=100K safe).
 */
public class QPartitionCrossover extends PartitionCrossover {

    private final MultiObjectiveLandscape moLandscape;
    private NonDominatedSet archive;

    private final int numObjectives;
    // O(1) lookup table: maps a sub-function index 'fn' to its objective index
    private final int[] objectiveMap;

    // Pre-allocated buffers to prevent Garbage Collection stalls
    private final double[] blueValBuffer;
    private final double[] redValBuffer;
    private final double[] alternativeDeltasFlat;

    // Primitive arrays to replace the memory-heavy HashSet<Integer> 'subfns'
    private final boolean[] affectedSubfnsBuffer;
    private final int[] touchedSubfnsBuffer;

    public QPartitionCrossover(MultiObjectiveLandscape el) {
        super(el);
        this.moLandscape = el;
        this.numObjectives = el.getNumObjectives();

        // Initialize buffers
        this.blueValBuffer = new double[numObjectives];
        this.redValBuffer = new double[numObjectives];
        this.alternativeDeltasFlat = new double[el.getN() * numObjectives];

        this.affectedSubfnsBuffer = new boolean[el.getM()];
        this.touchedSubfnsBuffer = new int[el.getM()];

        // Pre-compute the objective mapping for O(1) lookups during crossover
        this.objectiveMap = new int[el.getM()];
        List<Integer> mValues = el.getMValues();
        int currentObj = 0;
        int nextBoundary = mValues.get(0);

        for (int i = 0; i < el.getM(); i++) {
            if (i >= nextBoundary && currentObj < numObjectives - 1) {
                currentObj++;
                nextBoundary += mValues.get(currentObj);
            }
            objectiveMap[i] = currentObj;
        }
    }

    public void setArchive(NonDominatedSet archive) {
        this.archive = archive;
    }

    @Override
    public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
        long initTime = System.nanoTime();
        bfsSet.reset();

        PBSolution child = new PBSolution(red);
        numberOfComponents = 0;

        // Current Weights used by the problem (e.g., L1, L2, L3...)
        double[] currentWeights = moLandscape.getWeights();

        // 1. Calculate Baseline Weighted Fitness (Red Parent)
        // We allow exactly ONE array allocation here per crossover for tracking the best baseline
        double[] bestWeightedObjs = evaluateFullWeightedObjs(red);

        // 2. Iterate Components
        for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
            PartitionComponent component = bfs(node, blue, red);

            // Calculate Weighted Objectives directly into buffers (Zero Allocation)
            evaluateComponentWeightedObjs(component, blue, blueValBuffer);
            evaluateComponentWeightedObjs(component, red, redValBuffer);

            // Decision: Scalar Fitness (Sum of weighted objectives across N dimensions)
            double blueFitness = 0;
            double redFitness = 0;
            for (int i = 0; i < numObjectives; i++) {
                blueFitness += blueValBuffer[i];
                redFitness += redValBuffer[i];
            }

            // Tie-breaking
            boolean blueIsBest = (blueFitness > redFitness) ||
                    ((blueFitness == redFitness) && rnd.nextDouble() < 0.5);

            int flatIndexStart = numberOfComponents * numObjectives;

            if (blueIsBest) {
                // --- Blue is Best ---
                for (int variable : component) {
                    child.setBit(variable, blue.getBit(variable));
                    varProcedence.markAsBlue(variable);
                }

                // Update "Best" totals and store Alternative (Red) delta
                for (int i = 0; i < numObjectives; i++) {
                    bestWeightedObjs[i] += (blueValBuffer[i] - redValBuffer[i]);
                    alternativeDeltasFlat[flatIndexStart + i] = redValBuffer[i] - blueValBuffer[i];
                }

            } else {
                // --- Red is Best ---
                // Child bits stay Red. Best totals stay Red.
                // Store Alternative (Blue) delta
                for (int i = 0; i < numObjectives; i++) {
                    alternativeDeltasFlat[flatIndexStart + i] = blueValBuffer[i] - redValBuffer[i];
                }
            }

            numberOfComponents++;
        }

        // 3. Archive Strategy (Best + Neighbors)
        if (archive != null) {

            // A. Un-weight and Add Best Solution
            // Safe to allocate new arrays here because ParetoNonDominatedSet stores the array reference
            double[] bestRawObjs = new double[numObjectives];
            for (int i = 0; i < numObjectives; i++) {
                bestRawObjs[i] = (currentWeights[i] > 1e-9) ? bestWeightedObjs[i] / currentWeights[i] : 0.0;
            }
            archive.reportSolutionToArchive(bestRawObjs);

            // B. Un-weight and Add Neighbors (Hamming Distance 1)
            for (int c = 0; c < numberOfComponents; c++) {
                double[] neighborRawObjs = new double[numObjectives];
                int flatIndexStart = c * numObjectives;

                for (int i = 0; i < numObjectives; i++) {
                    double neighborWeightedObj = bestWeightedObjs[i] + alternativeDeltasFlat[flatIndexStart + i];
                    neighborRawObjs[i] = (currentWeights[i] > 1e-9) ? neighborWeightedObj / currentWeights[i] : 0.0;
                }
                archive.reportSolutionToArchive(neighborRawObjs);
            }
        }

        lastRuntime = System.nanoTime() - initTime;
        return child;
    }

    /**
     * Evaluates a component across N objectives natively writing to a pre-allocated buffer.
     * Uses primitive deduplication to prevent boxed Object creation.
     */
    private void evaluateComponentWeightedObjs(Iterable<Integer> vars, PBSolution sol, double[] outBuffer) {
        Arrays.fill(outBuffer, 0.0);
        int touchedCount = 0;

        // 1. Collect and deduplicate sub-functions using primitive arrays
        for (int variable : vars) {
            for (int fn : el.getAppearsIn()[variable]) {
                if (!affectedSubfnsBuffer[fn]) {
                    affectedSubfnsBuffer[fn] = true;
                    touchedSubfnsBuffer[touchedCount++] = fn;
                }
            }
        }

        // 2. Evaluate and reset the boolean tracking array simultaneously
        for (int i = 0; i < touchedCount; i++) {
            int fn = touchedSubfnsBuffer[i];

            double val = el.evaluateSubFunctionFromCompleteSolution(fn, sol);
            int objIndex = objectiveMap[fn]; // O(1) lookup
            outBuffer[objIndex] += val;

            // Instantly clean up the buffer for the next call without iterating the whole array
            affectedSubfnsBuffer[fn] = false;
        }
    }

    /**
     * Evaluates full solution across N objectives.
     */
    private double[] evaluateFullWeightedObjs(PBSolution sol) {
        double[] wObjs = new double[numObjectives];
        for (int i = 0; i < el.getM(); i++) {
            double val = el.evaluateSubFunctionFromCompleteSolution(i, sol);
            int objIndex = objectiveMap[i]; // O(1) lookup
            wObjs[objIndex] += val;
        }
        return wObjs;
    }
}