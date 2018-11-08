package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class VariableClique {
	
	private static final int DYNP_ITERATION_LIMIT = 29;
	private int variablesOfSeparator;
	private double [] summaryValue;
	private int [] variableValue;
	
	private List<Integer> variables;
	private VariableClique parent;
	private int id;
	private List<VariableClique> children;
	
	public VariableClique(int id) {
		this.id=id;
		children = new ArrayList<>();
	}
	
	public VariableClique getParent() {
		return parent;
	}
	public void setParent(VariableClique parent) {
		this.parent = parent;
		parent.children.add(this);
	}
	
	public List<Integer> getVariables() {
		if (variables==null) {
			variables = new ArrayList<>();
		}
		return variables;
	}
	
	public int getId() {
		return id;
	}
	
	public void prepareStructuresForComputation() {
		if (Math.max(variablesOfSeparator, variables.size()-variablesOfSeparator) > DYNP_ITERATION_LIMIT) {
			throw new RuntimeException("I cannot reduce this clique because it is too large");
		}
		summaryValue = new double [1<<variablesOfSeparator];
		variableValue = new int [1<<variablesOfSeparator];
	}

	public int getVariablesOfSeparator() {
		return variablesOfSeparator;
	}

	public double[] getSummaryValue() {
		return summaryValue;
	}

	public int[] getVariableValue() {
		return variableValue;
	}
	
	public Iterable<VariableClique> getChildren() {
		return children;
	}
	
	public int getSeparatorValueFromSolution(PBSolution solution) {
		int separatorValue = 0;
		for (int bit=0; bit < variablesOfSeparator; bit++) {
			separatorValue <<= 1;
			separatorValue += solution.getBit(variables.get(bit));
		}
		return separatorValue;
	}

	public double evaluateSolution(EmbeddedLandscape el, List<Integer>[] subFunctionsPartition, PBSolution solution) {
		double value = 0;
		for (VariableClique child: children) {
			int separatorValue = child.getSeparatorValueFromSolution(solution);
			value += child.summaryValue[separatorValue];
		}
		for (int i = variablesOfSeparator; i < variables.size(); i++) {
			int residueVariable = variables.get(i);
			for (int fn: subFunctionsPartition[residueVariable]) {
				value += el.evaluateSubFunctionFromCompleteSolution(fn, solution);
			}
		}
		return value;
	}

	public void markSeparator() {
		variablesOfSeparator = variables.size();
	}
	
	
}