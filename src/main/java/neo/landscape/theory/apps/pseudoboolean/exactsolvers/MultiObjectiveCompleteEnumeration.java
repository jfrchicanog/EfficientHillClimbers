package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;

public class MultiObjectiveCompleteEnumeration {
    private int unfeasibleSolutions;
    private ParetoNonDominatedSet nonDominatedSet;

    public ParetoNonDominatedSet solve(VectorMKLandscape pbf) {
        nonDominatedSet = new ParetoNonDominatedSet();
        unfeasibleSolutions=0;
        completeEnumeration(pbf);

        return nonDominatedSet;
    }

    public int getUnfeasibleSolutionsCount() {
        return unfeasibleSolutions;
    }

    private void completeEnumeration(VectorMKLandscape pbf) {
        int n = pbf.getN();
        PBSolution sol = new PBSolution(n);
        int[] data = sol.getData();

        if (n >= 31) {
            throw new RuntimeException("A long search of " + n
                + " bits. I will not do that!");
        }

        int limit = 1 << n;
        for (data[0] = 0; data[0] < limit; data[0]++) {
            double val [] = pbf.evaluate(sol);
            if (feasibleSolution(val, pbf.getConstraintIndex())) {
                nonDominatedSet.reportSolutionToArchive(val, pbf.getConstraintIndex());
            } else {
                unfeasibleSolutions++;
            }
        }

    }

    private boolean feasibleSolution(double[] val, int constraintIndex) {
        boolean feasible = true;
        for (int i = constraintIndex; feasible && i < val.length; i++) {
            feasible &= (val[i] >= 0);
        }
        return feasible;
    }

}
