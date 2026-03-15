package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.BiObjectiveLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Pareto Partition Crossover (PX) using Best + Neighbors sampling</b>
 *
 * <p>
 * This crossover strategy explores the local Pareto front by explicitly sampling
 * globally the best recombination and its immediate neighbors in component space.
 * Instead of enumerating all possible recombinations, it evaluates only a small,
 * targeted set of high-quality offspring.
 * </p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>
 *     Generate the single <b>best offspring</b>, corresponding to the global optimum
 *     under the current scalarization parameter.
 *   </li>
 *   <li>
 *     Generate <i>q</i> additional <b>neighboring offspring</b>, where <i>q</i> is the
 *     number of recombining components.
 *   </li>
 * </ol>
 *
 * <p>
 * Each neighboring offspring is obtained by taking the best offspring and flipping
 * exactly one component to its alternative parent source. These offspring are at
 * Hamming distance one from the best with respect to the crossover mask.
 * </p>
 *
 * <h3>Example (q = 4 components)</h3>
 *
 * <p>
 * Let <code>b1, b2, b3, b4</code> denote the best option for each component, and
 * let <code>a1, a2, a3, a4</code> denote the alternative (non-best) option.
 * </p>
 *
 * <p><b>Best offspring:</b></p>
 * <ul>
 *   <li><code>(b1, b2, b3, b4)</code></li>
 * </ul>
 *
 * <p><b>Neighboring offspring (Hamming distance 1):</b></p>
 * <ul>
 *   <li><code>(a1, b2, b3, b4)</code></li>
 *   <li><code>(b1, a2, b3, b4)</code></li>
 *   <li><code>(b1, b2, a3, b4)</code></li>
 *   <li><code>(b1, b2, b3, a4)</code></li>
 * </ul>
 *
 * <p>
 * <b>Guarantee:</b> By construction, this set contains the best offspring and its
 * highest-quality one-component deviations. As a result, the second- and third-best
 * offspring under the same scalarization are guaranteed to be included while
 * evaluating only <code>q + 1</code> offspring.
 * </p>
 *
 * <p>
 * Some of these offspring may lie on the Pareto front and can be obtained at
 * linear cost in the number of components.
 * </p>
 */
public class ParetoPartitionCrossover extends PartitionCrossover {

    private final int splitIndex;
    private ParetoNonDominatedSet archive;

    public ParetoPartitionCrossover(BiObjectiveLandscape el) {
        super(el);
        // Assuming mValues.get(0) is the number of subfunctions for Objective 1
        this.splitIndex = el.getMValues().get(0);
    }

    public void setArchive(ParetoNonDominatedSet archive) {
        this.archive = archive;
    }

    @Override
    public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
        long initTime = System.nanoTime();
        bfsSet.reset();

        PBSolution child = new PBSolution(red);
        numberOfComponents = 0;

        // Current Alpha used by the problem
        double currentAlpha = el.getAlpha();

        // 1. Calculate Baseline Weighted Fitness (Red Parent)
        // Note: The problem's evaluate function returns WEIGHTED values (val * alpha).
        double[] redWeightedFit = evaluateFullWeightedObj1Obj2(red);
        double bestWeightedObj1 = redWeightedFit[0];
        double bestWeightedObj2 = redWeightedFit[1];

        // Store deltas to generate neighbors later.
        // Each entry is { deltaWeightedObj1, deltaWeightedObj2 }
        List<double[]> alternativeDeltas = new ArrayList<>();

        // 2. Iterate Components
        for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
            PartitionComponent component = bfs(node, blue, red);

            // Calculate Weighted Objectives for both possibilities
            // These values ALREADY include alpha because the problem's evaluate() includes it.
            double[] blueVal = evaluateComponentWeightedObj1Obj2(component, blue);
            double[] redVal = evaluateComponentWeightedObj1Obj2(component, red);

            // Decision: Scalar Fitness (Sum of weighted objectives)
            // DO NOT multiply by alpha here; it was already done in the evaluation step.
            double blueFitness = blueVal[0] + blueVal[1];
            double redFitness = redVal[0] + redVal[1];

            // Tie-breaking
            boolean blueIsBest = (blueFitness > redFitness) ||
                    ((blueFitness == redFitness) && rnd.nextDouble() < 0.5);

            if (blueIsBest) {
                // --- Blue is Best ---
                for (int variable : component) {
                    child.setBit(variable, blue.getBit(variable));
                    varProcedence.markAsBlue(variable);
                }

                // Update "Best" totals (Child moves Red -> Blue)
                bestWeightedObj1 += (blueVal[0] - redVal[0]);
                bestWeightedObj2 += (blueVal[1] - redVal[1]);

                // Store Alternative (Red) as delta from Best (Blue)
                alternativeDeltas.add(new double[]{
                        redVal[0] - blueVal[0],
                        redVal[1] - blueVal[1]
                });

            } else {
                // --- Red is Best ---
                // Child bits stay Red.
                // Best totals stay Red.

                // Store Alternative (Blue) as delta from Best (Red)
                alternativeDeltas.add(new double[]{
                        blueVal[0] - redVal[0],
                        blueVal[1] - redVal[1]
                });
            }
            numberOfComponents++;
        }

        // 3. Archive Strategy (Best + Neighbors)
        // We must UN-WEIGHT the values before archiving to store raw objectives.
        if (archive != null) {

            // Helper to convert Weighted -> Raw safely
            double rawObj1 = (currentAlpha > 1e-9) ? bestWeightedObj1 / currentAlpha : 0.0;
            double rawObj2 = ((1 - currentAlpha) > 1e-9) ? bestWeightedObj2 / (1 - currentAlpha) : 0.0;

            // A. Add Best Solution
            archive.reportSolutionToArchive(new double[]{rawObj1, rawObj2});

            // B. Add Neighbors (Hamming Distance 1)
            for (double[] delta : alternativeDeltas) {
                double neighborWeightedObj1 = bestWeightedObj1 + delta[0];
                double neighborWeightedObj2 = bestWeightedObj2 + delta[1];

                double nRaw1 = (currentAlpha > 1e-9) ? neighborWeightedObj1 / currentAlpha : 0.0;
                double nRaw2 = ((1 - currentAlpha) > 1e-9) ? neighborWeightedObj2 / (1 - currentAlpha) : 0.0;

                archive.reportSolutionToArchive(new double[]{nRaw1, nRaw2});
            }
        }

        lastRuntime = System.nanoTime() - initTime;
        return child;
    }

    /**
     * Evaluates a component. Returns WEIGHTED values (e.g. RawObj1 * Alpha).
     */
    private double[] evaluateComponentWeightedObj1Obj2(Iterable<Integer> vars, PBSolution sol) {
        double wObj1 = 0;
        double wObj2 = 0;
        subfns.clear();

        for (int variable : vars) {
            for (int fn : el.getAppearsIn()[variable]) {
                subfns.add(fn);
            }
        }

        for (int fn : subfns) {
            // This returns (Alpha * SubFunctionValue) from NKLandscapes
            double val = el.evaluateSubFunctionFromCompleteSolution(fn, sol);
            if (fn < splitIndex) {
                wObj1 += val;
            } else {
                wObj2 += val;
            }
        }
        return new double[]{wObj1, wObj2};
    }

    /**
     * Evaluates full solution. Returns WEIGHTED values.
     */
    private double[] evaluateFullWeightedObj1Obj2(PBSolution sol) {
        double wObj1 = 0;
        double wObj2 = 0;
        for (int i = 0; i < el.getM(); i++) {
            double val = el.evaluateSubFunctionFromCompleteSolution(i, sol);
            if (i < splitIndex) {
                wObj1 += val;
            } else {
                wObj2 += val;
            }
        }
        return new double[]{wObj1, wObj2};
    }
}