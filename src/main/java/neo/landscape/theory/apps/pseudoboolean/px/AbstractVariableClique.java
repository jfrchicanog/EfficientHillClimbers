package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.DisjointSets;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class AbstractVariableClique implements VariableClique {

	@Override
	public VariableClique getParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setParent(VariableClique parent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Integer> getVariables() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addVariable(int var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAllVariables(Collection<Integer> vars) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void prepareStructuresForComputation(TwoStatesIntegerSet nonExhaustivelyExplored, DisjointSets disjointSets,
			Function<Integer, Integer> indexAssignment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getVariablesOfSeparator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void markSeparator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void applyDynamicProgrammingToClique(PBSolution red, EmbeddedLandscape embeddedLandscape,
			List<Integer>[] subFunctionPartitions, double[] summaryValue, int[] variableValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void reconstructSolutionInClique(PBSolution child, PBSolution red, VariableProcedence variableProcedence,
			int[] variableValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumberOfVariables() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getVariable(int index) {
		throw new UnsupportedOperationException();
	}

}
