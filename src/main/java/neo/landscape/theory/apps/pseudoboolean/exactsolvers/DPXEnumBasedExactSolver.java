package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.px.DynasticPotentialCrossover;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;

public class DPXEnumBasedExactSolver<P extends EmbeddedLandscape> implements ExactSolutionMethod<P>{
	
	public static class IncompleteExplorationException extends RuntimeException {
	}
	
	private int variablesToEnumerate;
	private DynasticPotentialCrossover dpx;
	private P problem;
	private PrintStream ps;
	private boolean debug;
	
	public DPXEnumBasedExactSolver(int varsToEnum) {
		variablesToEnumerate = varsToEnum;
	}
	
	public void setPrintStream (PrintStream ps) {
		this.ps = ps;
	}
	
	@Override
	public SolutionQuality<P> solveProblem(P el) {
		this.problem = el;
		dpx = new DynasticPotentialCrossover(problem);
		List<Integer> sortedVariables = sortedListOfVariables(problem);
		PBSolution red = new PBSolution(problem.getN());
		PBSolution blue = new PBSolution(red);
		for (int i=0; i < blue.getN(); i++) {
			blue.flipBit(i);
		}
		for (int i=0; i < variablesToEnumerate; i++) {
			blue.flipBit(sortedVariables.get(i));
		}
		
		SolutionQuality<P> solution = new SolutionQuality<>();
		try {
			exploreHyperplane(red, blue, solution);
			if (variablesToEnumerate > 0) {
				for (int bit : new GrayCodeBitFlipIterable(variablesToEnumerate)) {
					red.flipBit(sortedVariables.get(bit));
					blue.flipBit(sortedVariables.get(bit));
					exploreHyperplane(red, blue, solution);
				}
			}
			if (debug && ps  != null) {
				ps.println();
			}
		} catch (IncompleteExplorationException e) {
			return null;
		}

		return solution;
	}

	protected void exploreHyperplane(PBSolution red, PBSolution blue, SolutionQuality<P> solution) {
		PBSolution optimum = dpx.recombine(red, blue);
		double quality = problem.evaluate(optimum);
		if (dpx.getLogarithmOfExploredSolutions()!=(problem.getN()-variablesToEnumerate)) {
			throw new IncompleteExplorationException();
		} else if (solution.solution==null || solution.quality < quality) {
			solution.solution = optimum;
			solution.quality = quality;
		}
		if (debug && ps  != null) {
			ps.print(".");
			//System.out.print(".");
		}
	}
	
	private List<Integer> sortedListOfVariables(EmbeddedLandscape pbf) {
		int [][] interactions = pbf.getInteractions();
		Map<Integer, Integer> degreesOfVariables = new HashMap<>();
		for (int i=0; i < pbf.getN(); i++) {
			degreesOfVariables.put(i, interactions[i].length);
		}
		
		return degreesOfVariables.entrySet().stream()
			.sorted((e1,e2) -> e2.getValue().compareTo(e1.getValue()))
			.collect(Collectors.mapping(Entry::getKey, Collectors.toList()));
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
