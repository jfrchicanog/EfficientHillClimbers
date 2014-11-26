package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import java.math.BigDecimal;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.PseudoBooleanFunction;

public class CompleteEnumerationBigDecimal<P extends PseudoBooleanFunction>
		implements ExactSolutionMethod<PseudoBooleanFunction> {

	@Override
	public SolutionQuality<PseudoBooleanFunction> solveProblem(
			PseudoBooleanFunction problem) {

		int n = problem.getN();
		PBSolution sol = new PBSolution(n);
		int[] data = sol.getData();
		SolutionQuality<PseudoBooleanFunction> s = new SolutionQuality<PseudoBooleanFunction>();

		if (n >= 31) {
			throw new RuntimeException("A long search of " + n
					+ " bits. I will not do that!");
		}

		BigDecimal max = null;

		int limit = 1 << n;
		for (data[0] = 0; data[0] < limit; data[0]++) {
			BigDecimal val = problem.evaluateArbitraryPrecision(sol);
			if (max == null || val.compareTo(max) > 0) {
				max = val;
				s.solution = new PBSolution(sol);
			}
		}
		s.quality = max.doubleValue();

		System.out.println("Arbitrary precision optimal value: " + max);

		return s;
	}

}
