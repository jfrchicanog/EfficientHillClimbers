package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.px.DynasticPotentialCrossover;

public class DPXBasedExactSolver<P extends EmbeddedLandscape> implements ExactSolutionMethod<P>{
	@Override
	public SolutionQuality<P> solveProblem(P problem) {
		DynasticPotentialCrossover dpx = new DynasticPotentialCrossover(problem);
		PBSolution zeroes = new PBSolution(problem.getN());
		PBSolution ones = new PBSolution(zeroes);
		for (int i=0; i < ones.getN(); i++) {
			ones.flipBit(i);
		}
		
		PBSolution optimum = dpx.recombine(zeroes, ones);
		if (dpx.getLogarithmOfExploredSolutions()!=problem.getN()) {
			return null;
		}
		
		SolutionQuality<P> solution = new SolutionQuality<>();
		solution.solution = optimum;
		solution.quality = problem.evaluate(optimum);
		
		return solution;
	}

}
