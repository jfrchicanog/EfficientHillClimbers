package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Epsilon-Dominance Grid Archive</b>
 * * <p>
 * This class replaces the standard continuous Pareto archive to support massive
 * combinatorial landscapes (e.g., N=100K). It uses Epsilon-Dominance to divide the
 * objective space into a highly granular grid.
 * </p>
 * * <ul>
 * <li><b>Density & Bounding:</b> By converting continuous floating-point objectives into
 * discrete grid coordinates, it guarantees the archive size is mathematically capped.</li>
 * <li><b>Swap-and-Pop Deletions:</b> Employs an O(1) array swap technique when a solution
 * is dominated, completely avoiding the O(N) array shifts caused by Iterator.remove().</li>
 * </ul>
 */
public class NonDominatedSet {

    public enum DominanceRelation {
        DOMINATES, IS_DOMINATED, NON_DOMINATED, EQUAL
    }

    private final List<double[]> archive;

    // Cached multiplier to avoid slow division operations in the hot loop
    private final double invEpsilon;

    public NonDominatedSet(int n) {
        // Defines how many 'boxes' the theoretical maximum fitness is divided into.
        // Higher = denser Pareto front (more memory). Lower = sparser front (faster).
        double gridResolution = 500.0;
        double epsilon = (double) n / gridResolution;
        this.invEpsilon = 1.0 / epsilon;

        this.archive = new ArrayList<>(50000);
    }

    public void reportSolutionToArchive(double[] solutionToAdd) {
        reportSolutionToArchive(solutionToAdd, solutionToAdd.length);
    }

    public void reportSolutionToArchive(double[] solutionToAdd, int objectives) {
        int i = 0;

        // Zero-Allocation loop utilizing O(1) swap-and-pop deletions
        while (i < archive.size()) {
            double[] solutionQuality = archive.get(i);

            DominanceRelation rel = epsilonDominanceRelation(solutionToAdd, solutionQuality, objectives);

            if (rel == DominanceRelation.DOMINATES) {
                // O(1) Swap-and-Pop Deletion
                int lastIndex = archive.size() - 1;
                archive.set(i, archive.get(lastIndex));
                archive.remove(lastIndex);

                // Do NOT increment 'i'. The swapped element must be evaluated on the next cycle.
            } else if (rel == DominanceRelation.IS_DOMINATED || rel == DominanceRelation.EQUAL) {
                // Fast-fail: The new solution maps to the same grid box or is dominated.
                return;
            } else {
                // NON_DOMINATED: Move to the next element.
                i++;
            }
        }

        archive.add(solutionToAdd.clone());
    }

    /**
     * Calculates the Epsilon-Dominance relation by mapping raw objective values
     * to discrete integer grid coordinates. Maximization is assumed.
     */
    private DominanceRelation epsilonDominanceRelation(double[] firstSolution, double[] secondSolution, int objectives) {
        boolean firstBetter = false;
        boolean secondBetter = false;
        boolean sameBox = true;

        for (int i = 0; i < objectives; i++) {
            // Map continuous raw values to integer grid coordinates using fast multiplication
            int box1 = (int) (firstSolution[i] * invEpsilon);
            int box2 = (int) (secondSolution[i] * invEpsilon);

            if (box1 > box2) {
                firstBetter = true;
                sameBox = false;
            } else if (box1 < box2) {
                secondBetter = true;
                sameBox = false;
            }
        }

        if (sameBox) {
            // Both solutions occupy the exact same epsilon grid box.
            // In epsilon-dominance, we treat them as EQUAL and typically reject the new one
            // to cap computational growth.
            return DominanceRelation.EQUAL;
        } else if (firstBetter && !secondBetter) {
            return DominanceRelation.DOMINATES;
        } else if (!firstBetter) {
            return DominanceRelation.IS_DOMINATED;
        } else {
            return DominanceRelation.NON_DOMINATED;
        }
    }

    public String printArchive() {
        // Pre-allocate a large StringBuilder to avoid resizing during final flush
        StringBuilder result = new StringBuilder(archive.size() * 32 + 50);
        result.append("Archive (").append(archive.size()).append(" solutions):\n");
        for (double[] quality : archive) {
            printVector(result, quality);
        }
        return result.toString();
    }

    private void printVector(StringBuilder builder, double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            builder.append(vector[i]);
            if (i < vector.length - 1) {
                builder.append(", ");
            }
        }
        builder.append("\n");
    }

    public List<double[]> getSolutions() {
        return archive;
    }
}