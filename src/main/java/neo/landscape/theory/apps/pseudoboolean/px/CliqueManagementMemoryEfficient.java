package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableCliqueImplementation;

public class CliqueManagementMemoryEfficient implements CliqueManagement {
	private static class IndexAssigner implements Function<Integer,Integer> {
		private int index=0;
		@Override
		public Integer apply(Integer arraySize) {
			int thisIndex = index;
			index += arraySize;
			return thisIndex;
		}
		public int getIndex() {
			return index;
		}
		
		public void clearIndex() {
			index=0;
		}
	}
	
	public static final CliqueManagementFactory FACTORY = new CliqueManagementFactory() {
		@Override
		public CliqueManagement createCliqueManagement() {
			return new CliqueManagementMemoryEfficient();
		}
	};

	private List<VariableClique> cliques;
	private double [] summaryValue;
	private int [] variableValue;
	private final IndexAssigner indexAssigner = new IndexAssigner();
	private int numCliques;

	public CliqueManagementMemoryEfficient() {
		cliques = new ArrayList<>();
	}

	@Override
	public List<VariableClique> getCliques() {
		return cliques;
	}

	private void ensureSizeOfCliqueArrays(int size) {
		if (summaryValue == null || summaryValue.length < size) {
			summaryValue = new double [size];
			variableValue = new int [size];
		}
	}

	@Override
	public void applyDynamicProgramming(Set<Integer> nonExhaustivelyExploredVariables, int[] marks, PBSolution red, EmbeddedLandscape el, List<Integer> [] subFunctionsPartition) {
		indexAssigner.clearIndex();
		for (int i=numCliques-1; i>=0; i--) {
			cliques.get(i).prepareStructuresForComputation(nonExhaustivelyExploredVariables, marks, indexAssigner);
		}
		ensureSizeOfCliqueArrays(indexAssigner.getIndex());
		for (int i=numCliques-1; i>=0; i--) {
			cliques.get(i).applyDynamicProgrammingToClique(red, el, subFunctionsPartition, summaryValue, variableValue);
		}
	}

	@Override
	public void reconstructOptimalChild(PBSolution child, PBSolution red, VariableProcedence varProcedence) {
		for (VariableClique clique: cliques) {
			clique.reconstructSolutionInClique(child, red, varProcedence, variableValue);
		}
	}

	@Override
	public void clearCliqueTree() {
		cliques.clear();
		numCliques=0;
	}
	
	@Override
	public VariableClique addNewVariableClique() {
		VariableClique current = new VariableCliqueImplementation(numCliques++);
		cliques.add(current);
		return current;
	}

	@Override
	public void setVariableCliqueParent(int childID, int parentID) {
		cliques.get(childID).setParent(cliques.get(parentID));
	}
}