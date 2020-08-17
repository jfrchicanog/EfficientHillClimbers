package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class CliqueManagementMemoryEfficient implements CliqueManagement {
	public static final CliqueManagementFactory FACTORY = new CliqueManagementFactory() {
		@Override
		public CliqueManagement createCliqueManagement(int maxVariables) {
			return new CliqueManagementMemoryEfficient(maxVariables);
		}
	};
	
	private enum State {BUILD, QUERY};
	
	private class InternalVariableClique extends AbstractVariableClique {
		private int cliqueIndex;
		
		private InternalVariableClique (int cliqueId) {
			cliqueIndex = cliqueId;
		}

		@Override
		public int getNumberOfVariables() {
			return getNumberOfVariablesOfClique(cliqueIndex);
		}
		
		@Override
		public int getVariable(int index) {
			return getVariableOfClique(cliqueIndex, index);
		}
		
		@Override
		public List<Integer> getVariables() {
			// TODO: source of inefficiency
			return getVariablesOfClique(cliqueIndex);
		}
		
		@Override
		public VariableClique getParent() {
			// TODO source of ineficcency
			if (parent[cliqueIndex] < 0) {
				return null;
			}
			return new InternalVariableClique(parent[cliqueIndex]);
		}

		@Override
		public int getId() {
			return cliqueIndex;
		}

		@Override
		public int getVariablesOfSeparator() {
			return variablesOfSeparator[cliqueIndex];
		}
	}
	
	private class LastVariableClique extends AbstractVariableClique {
		@Override
		public int getNumberOfVariables() {
			return getNumberOfVariablesOfClique(numCliques-1);
		}
		
		@Override
		public int getVariable(int index) {
			return getVariableOfClique(numCliques-1, index);
		}

		@Override
		public int getId() {
			return numCliques-1;
		}

		@Override
		public int getVariablesOfSeparator() {
			return variablesOfSeparator[numCliques-1];
		}
		
		@Override
		public void addAllVariables(Collection<Integer> vars) {
			int newRequiredSize = vars.size()+variableIndex[numCliques-1]+1;
			if (newRequiredSize > variables.length) {
				variables = expandArray(variables, newRequiredSize);
			}
			for (int var: vars) {
				variables[++variableIndex[numCliques-1]]=var;
			}
		}
		
		@Override
		public void addVariable(int var) {
			int newRequiredSize = variableIndex[numCliques-1]+2;
			if (newRequiredSize > variables.length) {
				variables = expandArray(variables, newRequiredSize);
			}
			variables[++variableIndex[numCliques-1]]=var;
		}
		
		@Override
		public void markSeparator() {
			variablesOfSeparator[numCliques-1] = getNumberOfVariables();			
		}
	}
	
	private static final int DYNP_ITERATION_LIMIT = 29;
	
	private double [] summaryValue;
	private int [] variableValue;
	private final IndexAssigner indexAssigner = new IndexAssigner();
	private int numCliques;
	
	private int [] variablesOfSeparator;
	private int [] arraySize;
	private int [] arrayIndex;
	
	private int [] variableSeparatorLimit;
	private int [] variableResidueLimit;
	
	private int [] variables;  // this can be larger than the number of cliques
	private int [] variableIndex; // this is the last index of a variable of this VariableClique
	
	private int [] parent;
	private int [] children;
	private int [] childrenIndex;
	private BitSet sameGroupsOfNonExploredVariables;
	
	private State state;
	
	private final LastVariableClique lastVariableClique = new LastVariableClique();

	private TwoStatesIntegerSet nonExhaustivelyExploredVariables;
	private int groupsOfNonExhaustivelyExploredVariables;
	private TwoStatesIntegerSet articulationPoints;

	private CliqueManagementMemoryEfficient(int maxVariables) {
		int initialCliqueSize = Math.min(1, maxVariables >> 1);
		variablesOfSeparator = new int [initialCliqueSize];
		arraySize = new int [initialCliqueSize];
		arrayIndex = new int [initialCliqueSize];
		variableSeparatorLimit = new int [initialCliqueSize];
		variableResidueLimit = new int [initialCliqueSize];
		variables = new int [initialCliqueSize];
		variableIndex = new int [initialCliqueSize];
		parent = new int [initialCliqueSize];
		children = new int [initialCliqueSize];
		childrenIndex = new int [initialCliqueSize];
		sameGroupsOfNonExploredVariables = new BitSet(initialCliqueSize);
		nonExhaustivelyExploredVariables = new TwoStatesISArrayImpl(maxVariables);
		articulationPoints = new TwoStatesISArrayImpl(maxVariables);
		clearCliqueTree();
	}
	
	private static int [] expandArray(int [] array, int minNewSize) {
		int newSize;
		if (array.length <= 1) {
			newSize=2;
		} else {
			newSize = array.length+(array.length >> 1);
		}
		if (newSize < minNewSize) {
			newSize = minNewSize;
		}
		return Arrays.copyOf(array, newSize);
	}

	@Override
	public List<VariableClique> getCliques() {
		ensureQueryState();
		// TODO: this methods should be removed, to avoid many variables in the heap
		return IntStream.range(0, numCliques)
				.mapToObj(InternalVariableClique::new)
				.collect(Collectors.toList());
	}

	private void ensureSizeOfCliqueArrays(int size) {
		if (summaryValue == null || summaryValue.length < size) {
			summaryValue = new double [size];
			variableValue = new int [size];
		}
	}

	@Override
	public void applyDynamicProgramming(TwoStatesIntegerSet nonExhaustivelyExploredVariables, int[] marks, PBSolution red, EmbeddedLandscape el, List<Integer> [] subFunctionsPartition) {
		ensureQueryState();
		indexAssigner.clearIndex();
		for (int i=numCliques-1; i>=0; i--) {
			prepareStructuresForComputation(i, nonExhaustivelyExploredVariables, marks, indexAssigner);
		}
		ensureSizeOfCliqueArrays(indexAssigner.getIndex());
		for (int i=numCliques-1; i>=0; i--) {
			applyDynamicProgrammingToClique(i, red, el, subFunctionsPartition, summaryValue, variableValue);
		}
	}

	@Override
	public void reconstructOptimalChild(PBSolution child, PBSolution red, VariableProcedence varProcedence) {
		ensureQueryState();
		for (int clique=0; clique < numCliques; clique++) {
			reconstructSolutionInClique(clique, child, red, varProcedence, variableValue);
		}
	}

	@Override
	public void clearCliqueTree() {
		numCliques=0;
		groupsOfNonExhaustivelyExploredVariables=0;
		nonExhaustivelyExploredVariables.reset();
		articulationPoints.reset();
		state = State.BUILD;
	}
	
	@Override
	public VariableClique addNewVariableClique() {
		checkBuildState();
		if (numCliques >= parent.length) {
			parent = expandArray(parent, numCliques+1);
			variablesOfSeparator = expandArray(variablesOfSeparator, numCliques+1);
			arraySize = expandArray(arraySize, numCliques+1);
			arrayIndex = expandArray(arrayIndex, numCliques+1);
			variableSeparatorLimit = expandArray(variableSeparatorLimit, numCliques+1);
			variableResidueLimit = expandArray(variableResidueLimit, numCliques+1);
			children = expandArray(children, numCliques+1);
			variableIndex = expandArray(variableIndex, numCliques+1);
			childrenIndex = expandArray(childrenIndex, numCliques+1);
		}
		
		if (numCliques > 0) {
			variableIndex[numCliques] = variableIndex[numCliques-1];
		} else {
			variableIndex[numCliques] = -1;
		}
		parent[numCliques] = -1;
		childrenIndex[numCliques] = 0;
		
		numCliques++;
		return lastVariableClique;
	}

	@Override
	public void setVariableCliqueParent(int childID, int parentID) {
		checkBuildState();
		parent[childID] = parentID;
		childrenIndex[parentID]++;
	}

	private void checkBuildState() {
		if (state != State.BUILD) {
			throw new IllegalStateException("Modifying clique tree while not in BUILD state.");
		}
	}
	
	private int getNumberOfVariablesOfClique(int cliqueIndex) {
		if (cliqueIndex == 0) {
			return variableIndex[0]+1;
		} else {
			return variableIndex[cliqueIndex]-variableIndex[cliqueIndex-1];
		}
	}
	
	private int getVariableOfClique(int cliqueIndex, int index) {
		return variables[indexOfVariableInClique(cliqueIndex, index)];
	}
	
	private int indexOfVariableInClique(int cliqueIndex, int index) {
		if (cliqueIndex==0) {
			return index;
		} else {
			return index+variableIndex[cliqueIndex-1]+1;
		}
	}
	
	private List<Integer> getVariablesOfClique(int cliqueIndex) {
		return new AbstractList<Integer>() {
			@Override
			public Integer get(int index) {
				return getVariableOfClique(cliqueIndex, index);
			}

			@Override
			public int size() {
				return getNumberOfVariablesOfClique(cliqueIndex);
			}
			
			@Override
			public Integer set(int index, Integer element) {
				int varIndex = indexOfVariableInClique(cliqueIndex, index);
				int previousValue = variables[varIndex];
				variables[varIndex] = element;
				return previousValue;
			}
		};
	}
	
	private int getNumberOfChildrenOfClique(int cliqueIndex) {
		if (cliqueIndex == 0) {
			return childrenIndex[0]+1;
		} else {
			return childrenIndex[cliqueIndex]-childrenIndex[cliqueIndex-1];
		}
	}
	
	private int getChildrenOfClique(int cliqueIndex, int index) {
		if (cliqueIndex==0) {
			return children[index];
		} else {
			return children[index+childrenIndex[cliqueIndex-1]+1];
		}
	}
	
	private void ensureQueryState() {
		if (state != State.QUERY) {
			prepareDataSructureForQuery();
			state = State.QUERY;
		}
	}
	
	private void prepareDataSructureForQuery() {
		// Adjust indices
		int previousIndex = -1;
		for (int i=0; i < numCliques; i++) {
			int newIndex = previousIndex + childrenIndex[i];
			childrenIndex[i] = previousIndex;
			previousIndex = newIndex;
		}
		
		for (int i=0; i < numCliques; i++) {
			int p = parent[i];
			if (p >= 0) {
				children[++childrenIndex[p]] = i;
			}
		}
	}

	private void applyDynamicProgrammingToClique(int clique, PBSolution red, EmbeddedLandscape embeddedLandscape, List<Integer>[] subFunctionPartitions, double[] summaryValue, int[] variableValue) {
		PBSolution solution = new PBSolution(red);
		
		int separatorValueLimit = arraySize[clique];
		int numVariablesOfResidue = getNumberOfVariablesOfClique(clique)-variablesOfSeparator[clique];
		int residueValueLimit = 1 << variableResidueLimit[clique];

		if (variableResidueLimit[clique] < numVariablesOfResidue && !sameGroupsOfNonExploredVariables.get(clique)) {
			residueValueLimit <<= 1;
		}
		
		// Iterate over the variables in the separator 
		for (int separatorValue=0; separatorValue < separatorValueLimit; separatorValue++) {
			int auxSeparator=separatorValue;
			for (int bit=0; bit < variableSeparatorLimit[clique]; bit++) {
				solution.setBit(getVariableOfClique(clique, bit), auxSeparator & 1);
				auxSeparator >>>= 1;
			}
			if (variableSeparatorLimit[clique] < variablesOfSeparator[clique]) {
				if ((auxSeparator & 1)==0) {
					// Red solution
					for (int bit=variableSeparatorLimit[clique]; bit < variablesOfSeparator[clique]; bit++) {
						int variable = getVariableOfClique(clique, bit);
						solution.setBit(variable, red.getBit(variable));
					}
				} else {
					// Blue solution
					for (int bit = variableSeparatorLimit[clique]; bit < variablesOfSeparator[clique]; bit++) {
						int variable = getVariableOfClique(clique, bit);
						solution.setBit(variable, 1 - red.getBit(variable));
					}
				}
			}
			
			summaryValue[arrayIndex[clique]+separatorValue]=Double.NEGATIVE_INFINITY;
			
			// Iterate over the variables in the residue
			for (int residueValue=0; residueValue < residueValueLimit; residueValue++) {
				int auxResidue=residueValue;
				for (int bit=0; bit < variableResidueLimit[clique]; bit++) {
					solution.setBit(getVariableOfClique(clique, variablesOfSeparator[clique]+bit), auxResidue & 1);
					auxResidue >>>= 1;
				}
				if (variableResidueLimit[clique] < numVariablesOfResidue) {
					if (sameGroupsOfNonExploredVariables.get(clique)) {
						auxResidue = auxSeparator;
					}
					
					if ((auxResidue & 1) == 0) {
						// Red solution
						for (int bit=variableResidueLimit[clique]; bit < numVariablesOfResidue; bit++) {
							int variable = getVariableOfClique(clique, variablesOfSeparator[clique]+bit);
							solution.setBit(variable, red.getBit(variable));
						}
					} else {
						// Blue solution
						for (int bit=variableResidueLimit[clique]; bit < numVariablesOfResidue; bit++) {
							int variable = getVariableOfClique(clique, variablesOfSeparator[clique]+bit);
							solution.setBit(variable, 1 - red.getBit(variable));
						}
					}
				}
				
				// We have the solution here and we have to evaluate it
				double value = evaluateSolution(clique, embeddedLandscape, subFunctionPartitions, solution, red, summaryValue);
				if (value > summaryValue[arrayIndex[clique]+separatorValue]) {
					summaryValue[arrayIndex[clique]+separatorValue] = value;
					variableValue[arrayIndex[clique] + separatorValue] = residueValue;
				}
			}
		}
	}
	
	private int getSeparatorValueFromSolution(int clique, PBSolution solution, PBSolution red) {
		int separatorValue = 0;
		if (variableSeparatorLimit[clique] < variablesOfSeparator[clique]) {
			int variable = getVariableOfClique(clique, variableSeparatorLimit[clique]);
			if (solution.getBit(variable) == red.getBit(variable)) {
				separatorValue = 0;
			} else {
				separatorValue = 1;
			}
		}
		
		for (int bit=variableSeparatorLimit[clique]-1; bit >= 0; bit--) {
			separatorValue <<= 1;
			separatorValue += solution.getBit(getVariableOfClique(clique, bit));
		}
		return separatorValue;
	}
	
	private double evaluateSolution(int clique, EmbeddedLandscape el, List<Integer>[] subFunctionsPartition, PBSolution solution, PBSolution red, double [] summaryValue) {
		double value = 0;
		for (int childIndex = 0; childIndex < getNumberOfChildrenOfClique(clique); childIndex++) {
			int child = getChildrenOfClique(clique, childIndex);
			int separatorValue = getSeparatorValueFromSolution(child, solution, red);
			value += summaryValue[arrayIndex[child] + separatorValue];
		}
		
		for (int i = variablesOfSeparator[clique]; i < getNumberOfVariablesOfClique(clique); i++) {
			int residueVariable = getVariableOfClique(clique, i);
			for (int fn: subFunctionsPartition[residueVariable]) {
				value += el.evaluateSubFunctionFromCompleteSolution(fn, solution);
			}
		}
		return value;
	}
	
	private void prepareStructuresForComputation(int clique, TwoStatesIntegerSet nonExhaustivelyExplored, int [] marks, Function<Integer,Integer> indexAssignment) {
		List<Integer> listVariables = getVariablesOfClique(clique);
		
		List<Integer> separator = listVariables.subList(0, variablesOfSeparator[clique]);
		variableSeparatorLimit[clique] = variableLimitFromList(nonExhaustivelyExplored, separator);
		
		List<Integer> residue = listVariables.subList(variablesOfSeparator[clique], getNumberOfVariablesOfClique(clique));
		variableResidueLimit[clique] = variableLimitFromList(nonExhaustivelyExplored, residue);
		
		if (Math.max(variableSeparatorLimit[clique], variableResidueLimit[clique]) > DYNP_ITERATION_LIMIT) {
			throw new RuntimeException("I cannot reduce this clique because it is too large (Reduce the exhaustive exploration)");
		}
		
		arraySize[clique] = 1<< variableSeparatorLimit[clique];
		if (variableSeparatorLimit[clique] < variablesOfSeparator[clique]) {
			arraySize[clique] <<= 1;
		}
		
		arrayIndex[clique] = indexAssignment.apply(arraySize[clique]);
		
		int numVariablesOfResidue = getNumberOfVariablesOfClique(clique)-variablesOfSeparator[clique];
		boolean sameGroup = (variableResidueLimit[clique] < numVariablesOfResidue) &&
				(variableSeparatorLimit[clique] < variablesOfSeparator[clique]) &&
				(marks[getVariableOfClique(clique, variableSeparatorLimit[clique])] == marks[getVariableOfClique(clique,variablesOfSeparator[clique]+variableResidueLimit[clique])]);
		
		
		sameGroupsOfNonExploredVariables.set(clique, sameGroup);
	}

	private static int variableLimitFromList(TwoStatesIntegerSet nonExhaustivelyExplored, List<Integer> separator) {
		separator.sort(Comparator.<Integer>comparingInt(variable->nonExhaustivelyExplored.isExplored(variable)?1:0));
		int i;
		for (i=0; i < separator.size() && !nonExhaustivelyExplored.isExplored(separator.get(i)); i++);
		return i;
	}
	
	private void reconstructSolutionInClique(int clique, PBSolution child, PBSolution red, VariableProcedence variableProcedence, int [] variableValue) {
		int numVariablesOfResidue = getNumberOfVariablesOfClique(clique)-variablesOfSeparator[clique];
		int separatorValue = getSeparatorValueFromSolution(clique, child, red);
		int residueVariables = variableValue[arrayIndex[clique]+separatorValue];
		
		for (int bit=0; bit < variableResidueLimit[clique]; bit++) {
			Integer variable = getVariableOfClique(clique, variablesOfSeparator[clique]+bit);
			child.setBit(variable, residueVariables & 1);
			
			if (red.getBit(variable) != (residueVariables & 1)) {
				variableProcedence.markAsBlue(variable);
			}
			residueVariables >>>= 1;
		}
		if (variableResidueLimit[clique] < numVariablesOfResidue) {
			if (sameGroupsOfNonExploredVariables.get(clique)) {
				residueVariables = ((separatorValue >>> variableSeparatorLimit[clique]) & 1);
			}
			if ((residueVariables & 1) == 0) {
				// Red solution
				for (int bit=variableResidueLimit[clique]; bit < numVariablesOfResidue; bit++) {
					int variable = getVariableOfClique(clique,variablesOfSeparator[clique]+bit);
					child.setBit(variable, red.getBit(variable));
				}
			} else {
				// Blue solution
				for (int bit=variableResidueLimit[clique]; bit < numVariablesOfResidue; bit++) {
					int variable = getVariableOfClique(clique, variablesOfSeparator[clique]+bit);
					child.setBit(variable, 1 - red.getBit(variable));
					variableProcedence.markAsBlue(variable);
				}
			}
		}
	}
	
	@Override
	public String getCliqueTree() {
		String result = "";
		for (int clique=0; clique < numCliques; clique++) {
			int vars = getNumberOfVariablesOfClique(clique);
			int sepVars = variablesOfSeparator[clique];

			Set<Integer> separator = new HashSet<>();
			Set<Integer> residue = new HashSet<>();
			int v;
			for (v=0; v < sepVars; v++) {
				separator.add(getVariableOfClique(clique, v));
			}
			for (; v < vars; v++) {
				residue.add(getVariableOfClique(clique, v));
			}
			result += "Clique "+clique+" (parent "+parent[clique]+"): separator="+separator+ ", residue="+residue+"\n";
		}
		return result;
	}
	
	@Override
	public TwoStatesIntegerSet getNonExhaustivelyExploredVariables() {
		return nonExhaustivelyExploredVariables;
	}
	
	@Override
	public int getGroupsOfNonExhaustivelyExploredVariables() {
		return groupsOfNonExhaustivelyExploredVariables;
	}

	@Override
	public void cliqueTreeAnalysis(DynasticPotentialCrossover dynasticPotentialCrossover) {
		getNonExhaustivelyExploredVariables().reset();
		for (VariableClique clique: getCliques()) {
			assert orderOfVariablesInClique(dynasticPotentialCrossover, clique.getVariables());
			
			List<Integer> listOfVariables = clique.getVariables().subList(0,clique.getVariablesOfSeparator());
			checkExplorationLimits(dynasticPotentialCrossover, listOfVariables);
			
			listOfVariables = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			checkExplorationLimits(dynasticPotentialCrossover, listOfVariables);
		}
		
		analysisOfGroupsOfNonExhaustivelyExploredVariables(dynasticPotentialCrossover);
		
	}
	
	private void analysisOfGroupsOfNonExhaustivelyExploredVariables(DynasticPotentialCrossover dynasticPotentialCrossover) {
		List<Set<Integer>> partitionOfNonExploredVariables = new ArrayList<>();
		for (VariableClique clique: getCliques()) {
			List<Integer> separator = clique.getVariables().subList(0, clique.getVariablesOfSeparator());
			List<Integer> residue = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			for (List<Integer> listOfVariables : new List[] { separator, residue }) {
				Set<Integer> newComponent = listOfVariables.stream().filter(getNonExhaustivelyExploredVariables()::isExplored)
						.collect(Collectors.toSet());
				
				if (newComponent.isEmpty()) {
					continue;
				}
				
				Iterator<Set<Integer>> it = partitionOfNonExploredVariables.iterator();
				Set<Integer> updatedComponent = null;
				while (it.hasNext()) {
					Set<Integer> previousComponent = it.next();
					if (previousComponent.stream().filter(newComponent::contains).findFirst().isPresent()) {
						if (updatedComponent == null) {
							updatedComponent = previousComponent;
							updatedComponent.addAll(newComponent);
						} else {
							updatedComponent.addAll(previousComponent);
							it.remove();
						}
					}
				}
				if (updatedComponent == null) {
					partitionOfNonExploredVariables.add(newComponent);
				}
			}
		}
		groupsOfNonExhaustivelyExploredVariables = partitionOfNonExploredVariables.size();
		
		if (dynasticPotentialCrossover.debug) {
			Set<Integer> auxiliar = new HashSet<>();
			for (Set<Integer> component: partitionOfNonExploredVariables) {
				assert auxiliar.stream().filter(component::contains).count()==0;
				auxiliar.addAll(component);
			}
			assert auxiliar.equals(getNonExhaustivelyExploredVariables().getExplored().boxed().collect(Collectors.toSet()));
		}
		
		for (int i=0; i < partitionOfNonExploredVariables.size(); i++) {
			Set<Integer> component = partitionOfNonExploredVariables.get(i);
			for (int variable : component) {
				dynasticPotentialCrossover.marks[variable] = i;
			}
		}
	}
	
	private void checkExplorationLimits(DynasticPotentialCrossover dynasticPotentialCrossover, List<Integer> listOfVariables) {
		if (listOfVariables.size() > dynasticPotentialCrossover.maximumNumberOfVariableToExploreExhaustively) {
			listOfVariables.sort(Comparator.<Integer>comparingInt(variable -> articulationPoints.isExplored(variable)?0:1)
					.thenComparing(Comparator.<Integer>naturalOrder()));
			listOfVariables.subList(dynasticPotentialCrossover.maximumNumberOfVariableToExploreExhaustively, listOfVariables.size())
			.forEach(getNonExhaustivelyExploredVariables()::explored);
			
		}
	}
	
	private boolean orderOfVariablesInClique(DynasticPotentialCrossover dynasticPotentialCrossover, List<Integer> list) {
		List<Integer> alphas = list.stream().mapToInt(v->dynasticPotentialCrossover.alpha[v]).boxed().collect(Collectors.toList());
		if (alphas.size()>2) {
			int val = alphas.get(0);
			for (int i=1; i < alphas.size(); i++) {
				if (alphas.get(i) > val) {
					return false;
				}
				val = alphas.get(i);
			}
		}
		return true;
	}
	
	@Override
	public boolean allArticulationPointsExhaustivelyExplored(DynasticPotentialCrossover dynasticPotentialCrossover) {
		return !getNonExhaustivelyExploredVariables().getExplored().anyMatch(articulationPoints::isExplored);
	}

	@Override
	public void addArticulationPoint(int variable) {
		articulationPoints.explored(variable);
	}

	@Override
	public int getNumberOfArticulationPoints() {
		return  articulationPoints.getNumberOfExploredElements();
	}
}