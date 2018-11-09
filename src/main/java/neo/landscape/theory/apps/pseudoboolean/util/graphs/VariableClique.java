package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.px.VariableProcedence;

public class VariableClique {
	
	private static final int DYNP_ITERATION_LIMIT = 29;
	private int variablesOfSeparator;
	private double [] summaryValue;
	private int [] variableValue;
	
	private int variableSeparatorLimit;
	private int variableResidueLimit;
	
	private List<Integer> variables;
	private VariableClique parent;
	private int id;
	private List<VariableClique> children;
	private boolean sameGroupsOfNonExploredVariables;
	
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
	
	public void prepareStructuresForComputation(Set<Integer> nonExhaustivelyExplored, int [] marks) {
		List<Integer> separator = variables.subList(0, variablesOfSeparator);
		variableSeparatorLimit = variableLimitFromList(nonExhaustivelyExplored, separator);
		
		List<Integer> residue = variables.subList(variablesOfSeparator, variables.size());
		variableResidueLimit = variableLimitFromList(nonExhaustivelyExplored, residue);
		
		if (Math.max(variableSeparatorLimit, variableResidueLimit) > DYNP_ITERATION_LIMIT) {
			throw new RuntimeException("I cannot reduce this clique because it is too large (Reduce the exhaustive exploration)");
		}
		
		int arraySize = 1<<variableSeparatorLimit;
		if (variableSeparatorLimit < variablesOfSeparator) {
			arraySize <<= 1;
		}
		
		summaryValue = new double [arraySize];
		variableValue = new int [arraySize];
		
		int numVariablesOfResidue = variables.size()-variablesOfSeparator;
		sameGroupsOfNonExploredVariables = (variableResidueLimit < numVariablesOfResidue) &&
				(variableSeparatorLimit < variablesOfSeparator) &&
				(marks[variables.get(variableSeparatorLimit)] == marks[variables.get(variablesOfSeparator+variableResidueLimit)]);
	}

	protected int variableLimitFromList(Set<Integer> nonExhaustivelyExplored, List<Integer> separator) {
		separator.sort(Comparator.<Integer>comparingInt(variable->nonExhaustivelyExplored.contains(variable)?1:0));
		int i;
		for (i=0; i < separator.size() && !nonExhaustivelyExplored.contains(separator.get(i)); i++);
		return i;
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
	
	public int getSeparatorValueFromSolution(PBSolution solution, PBSolution red) {
		int separatorValue = 0;
		if (variableSeparatorLimit < variablesOfSeparator) {
			int variable = variables.get(variableSeparatorLimit);
			if (solution.getBit(variable) == red.getBit(variable)) {
				separatorValue = 0;
			} else {
				separatorValue = 1;
			}
		}
		
		for (int bit=variableSeparatorLimit-1; bit >= 0; bit--) {
			separatorValue <<= 1;
			separatorValue += solution.getBit(variables.get(bit));
		}
		return separatorValue;
	}

	public double evaluateSolution(EmbeddedLandscape el, List<Integer>[] subFunctionsPartition, PBSolution solution, PBSolution red) {
		double value = 0;
		for (VariableClique child: children) {
			int separatorValue = child.getSeparatorValueFromSolution(solution, red);
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

	public void applyDynamicProgrammingToClique(PBSolution red, EmbeddedLandscape embeddedLandscape, List<Integer>[] subFunctionPartitions) {
		PBSolution solution = new PBSolution(red);
		
		int separatorValueLimit = summaryValue.length;
		int numVariablesOfResidue = variables.size()-variablesOfSeparator;
		int residueValueLimit = 1 << variableResidueLimit;

		if (variableResidueLimit < numVariablesOfResidue && !sameGroupsOfNonExploredVariables) {
			residueValueLimit <<= 1;
		}
		
		// Iterate over the variables in the separator 
		for (int separatorValue=0; separatorValue < separatorValueLimit; separatorValue++) {
			int auxSeparator=separatorValue;
			for (int bit=0; bit < variableSeparatorLimit; bit++) {
				solution.setBit(variables.get(bit), auxSeparator & 1);
				auxSeparator >>>= 1;
			}
			if (variableSeparatorLimit < variablesOfSeparator) {
				if ((auxSeparator & 1)==0) {
					// Red solution
					for (int bit=variableSeparatorLimit; bit < variablesOfSeparator; bit++) {
						int variable = variables.get(bit);
						solution.setBit(variable, red.getBit(variable));
					}
				} else {
					// Blue solution
					for (int bit = variableSeparatorLimit; bit < variablesOfSeparator; bit++) {
						int variable = variables.get(bit);
						solution.setBit(variable, 1 - red.getBit(variable));
					}
				}
			}
			
			summaryValue[separatorValue]=Double.NEGATIVE_INFINITY;
			
			// Iterate over the variables in the residue
			for (int residueValue=0; residueValue < residueValueLimit; residueValue++) {
				int auxResidue=residueValue;
				for (int bit=0; bit < variableResidueLimit; bit++) {
					solution.setBit(variables.get(variablesOfSeparator+bit), auxResidue & 1);
					auxResidue >>>= 1;
				}
				if (variableResidueLimit < numVariablesOfResidue) {
					if (sameGroupsOfNonExploredVariables) {
						auxResidue = auxSeparator;
					}
					
					if ((auxResidue & 1) == 0) {
						// Red solution
						for (int bit=variableResidueLimit; bit < numVariablesOfResidue; bit++) {
							int variable = variables.get(variablesOfSeparator+bit);
							solution.setBit(variable, red.getBit(variable));
						}
					} else {
						// Blue solution
						for (int bit=variableResidueLimit; bit < numVariablesOfResidue; bit++) {
							int variable = variables.get(variablesOfSeparator+bit);
							solution.setBit(variable, 1 - red.getBit(variable));
						}
					}
				}
				
				// We have the solution here and we have to evaluate it
				double value = evaluateSolution(embeddedLandscape, subFunctionPartitions, solution, red);
				if (value > summaryValue[separatorValue]) {
					summaryValue[separatorValue] = value;
					variableValue[separatorValue] = residueValue;
				}
			}
		}
	}

	public void reconstructSolutionInClique(PBSolution child, PBSolution red, VariableProcedence variableProcedence) {
		int numVariablesOfResidue = variables.size()-variablesOfSeparator;
		int separatorValue = getSeparatorValueFromSolution(child, red);
		int residueVariables = variableValue[separatorValue];
		
		for (int bit=0; bit < variableResidueLimit; bit++) {
			Integer variable = variables.get(variablesOfSeparator+bit);
			child.setBit(variable, residueVariables & 1);
			
			if (red.getBit(variable) != (residueVariables & 1)) {
				variableProcedence.markAsBlue(variable);
			}
			residueVariables >>>= 1;
		}
		if (variableResidueLimit < numVariablesOfResidue) {
			if (sameGroupsOfNonExploredVariables) {
				residueVariables = ((separatorValue >>> variableSeparatorLimit) & 1);
			}
			if ((residueVariables & 1) == 0) {
				// Red solution
				for (int bit=variableResidueLimit; bit < numVariablesOfResidue; bit++) {
					int variable = variables.get(variablesOfSeparator+bit);
					child.setBit(variable, red.getBit(variable));
				}
			} else {
				// Blue solution
				for (int bit=variableResidueLimit; bit < numVariablesOfResidue; bit++) {
					int variable = variables.get(variablesOfSeparator+bit);
					child.setBit(variable, 1 - red.getBit(variable));
					variableProcedence.markAsBlue(variable);
				}
			}
		}
	}
	
	
}