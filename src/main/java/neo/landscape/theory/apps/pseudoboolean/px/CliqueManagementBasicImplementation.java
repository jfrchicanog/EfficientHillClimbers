package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableCliqueImplementation;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class CliqueManagementBasicImplementation implements CliqueManagement {	
	public static final CliqueManagementFactory FACTORY = new CliqueManagementFactory() {
		@Override
		public CliqueManagement createCliqueManagement(int estimatedSize) {
			return new CliqueManagementBasicImplementation();
		}
	};

	private List<VariableClique> cliques;
	private double [] summaryValue;
	private int [] variableValue;
	private final IndexAssigner indexAssigner = new IndexAssigner();
	private int numCliques;

	private CliqueManagementBasicImplementation() {
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
	public void applyDynamicProgramming(TwoStatesIntegerSet nonExhaustivelyExploredVariables, int[] marks, PBSolution red, EmbeddedLandscape el, List<Integer> [] subFunctionsPartition) {
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
	
	@Override
	public String getCliqueTree() {
		String result = "";
		for (VariableClique clique: getCliques()) {
			Set<Integer> residue = new HashSet<>();
			residue.addAll(clique.getVariables());
			
			if (clique.getParent() != null) {
				residue.removeAll(clique.getParent().getVariables());
			}
			Set<Integer> separator = new HashSet<>();
			separator.addAll(clique.getVariables());
			separator.removeAll(residue);
			result += "Clique "+clique.getId()+" (parent "+(clique.getParent()!=null?clique.getParent().getId():-1)+"): separator="+separator+ ", residue="+residue+"\n";
		}
		return result;
	}
}