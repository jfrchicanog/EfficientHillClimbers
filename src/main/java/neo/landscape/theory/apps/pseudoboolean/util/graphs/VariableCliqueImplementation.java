package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.px.VariableProcedence;
import neo.landscape.theory.apps.pseudoboolean.util.DisjointSets;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class VariableCliqueImplementation implements VariableClique {
	
	private static final int DYNP_ITERATION_LIMIT = 29;
	private int variablesOfSeparator;
	private long arraySize;
	private long arrayIndex;
	
	private int variableSeparatorLimit;
	private int variableResidueLimit;
	
	private List<Integer> variables;
	private VariableClique parent;
	private int id;
	private List<VariableCliqueImplementation> children;
	private boolean sameGroupsOfNonExploredVariables;
	
	public VariableCliqueImplementation(int id) {
		this.id=id;
		children = new ArrayList<>();
	}
	
	@Override
	public VariableClique getParent() {
		return parent;
	}
	
	@Override
	public void setParent(VariableClique parent) {
		this.parent = parent;
		((VariableCliqueImplementation)parent).children.add(this);
	}
	
	@Override
	public List<Integer> getVariables() {
		if (variables==null) {
			variables = new ArrayList<>();
		}
		return variables;
	}
	
	@Override
	public int getId() {
		return id;
	}
	
	@Override
	public void prepareStructuresForComputation(TwoStatesIntegerSet nonExhaustivelyExplored, DisjointSets disjointSets, Function<Long,Long> indexAssignment) {
		List<Integer> separator = variables.subList(0, variablesOfSeparator);
		variableSeparatorLimit = variableLimitFromList(nonExhaustivelyExplored, separator);
		
		List<Integer> residue = variables.subList(variablesOfSeparator, variables.size());
		variableResidueLimit = variableLimitFromList(nonExhaustivelyExplored, residue);
		
		if (Math.max(variableSeparatorLimit, variableResidueLimit) > DYNP_ITERATION_LIMIT) {
			throw new RuntimeException("I cannot reduce this clique because it is too large (Reduce the exhaustive exploration)");
		}
		
		arraySize = 1<<variableSeparatorLimit;
		if (variableSeparatorLimit < variablesOfSeparator) {
			arraySize <<= 1;
		}
		
		arrayIndex = indexAssignment.apply(arraySize);
		
		int numVariablesOfResidue = variables.size()-variablesOfSeparator;
		sameGroupsOfNonExploredVariables = (variableResidueLimit < numVariablesOfResidue) &&
				(variableSeparatorLimit < variablesOfSeparator) &&
				(disjointSets.sameSet(variables.get(variableSeparatorLimit),variables.get(variablesOfSeparator+variableResidueLimit)));
	}

	private static int variableLimitFromList(TwoStatesIntegerSet nonExhaustivelyExplored, List<Integer> separator) {
		separator.sort(Comparator.<Integer>comparingInt(variable->nonExhaustivelyExplored.isExplored(variable)?1:0));
		int i;
		for (i=0; i < separator.size() && !nonExhaustivelyExplored.isExplored(separator.get(i)); i++);
		return i;
	}

	@Override
	public int getVariablesOfSeparator() {
		return variablesOfSeparator;
	}
	
	private int getSeparatorValueFromSolution(PBSolution solution, PBSolution red) {
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

	private double evaluateSolution(EmbeddedLandscape el, List<Integer>[] subFunctionsPartition, PBSolution solution, PBSolution red, double [] summaryValue) {
		double value = 0;
		for (VariableCliqueImplementation child: children) {
			int separatorValue = child.getSeparatorValueFromSolution(solution, red);
			value += summaryValue[(int)(child.arrayIndex + separatorValue)];
		}
		for (int i = variablesOfSeparator; i < variables.size(); i++) {
			int residueVariable = variables.get(i);
			for (int fn: subFunctionsPartition[residueVariable]) {
				value += el.evaluateSubFunctionFromCompleteSolution(fn, solution);
			}
		}
		return value;
	}

	@Override
	public void markSeparator() {
		variablesOfSeparator = variables.size();
	}

	@Override
	public void applyDynamicProgrammingToClique(PBSolution red, EmbeddedLandscape embeddedLandscape, List<Integer>[] subFunctionPartitions, double[] summaryValue, int[] variableValue) {
		PBSolution solution = new PBSolution(red);
		
		long separatorValueLimit = arraySize;
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
			
			summaryValue[(int)(arrayIndex+separatorValue)]=Double.NEGATIVE_INFINITY;
			
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
				double value = evaluateSolution(embeddedLandscape, subFunctionPartitions, solution, red, summaryValue);
				if (value > summaryValue[(int)(arrayIndex+separatorValue)]) {
					summaryValue[(int)(arrayIndex+separatorValue)] = value;
					variableValue[(int)(arrayIndex + separatorValue)] = residueValue;
				}
			}
		}
	}

	@Override
	public void reconstructSolutionInClique(PBSolution child, PBSolution red, VariableProcedence variableProcedence, int [] variableValue) {
		int numVariablesOfResidue = variables.size()-variablesOfSeparator;
		int separatorValue = getSeparatorValueFromSolution(child, red);
		int residueVariables = variableValue[(int)(arrayIndex+separatorValue)];
		
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

	@Override
	public void addVariable(int var) {
		getVariables().add(var);
	}

	@Override
	public void addAllVariables(Collection<Integer> vars) {
		getVariables().addAll(vars);
	}

	@Override
	public int getNumberOfVariables() {
		return getVariables().size();
	}

	@Override
	public int getVariable(int index) {
		return getVariables().get(index);
	}
	
	
}