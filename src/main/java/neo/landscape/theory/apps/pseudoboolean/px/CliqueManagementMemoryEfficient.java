package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.DisjointSets;
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
	private long [] arraySize;
	private long [] arrayIndex;
	
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
	private int maximumVariablesToExhaustivelyExplore;
	
	private int [] auxiliaryArray; // iff possible, this could come from the arrays of Dynastic potential class
	
	private boolean debug;
	private DisjointSets disjointSets;

	private CliqueManagementMemoryEfficient(int maxVariables) {
		int initialCliqueSize = Math.min(1, maxVariables >> 1);
		variablesOfSeparator = new int [initialCliqueSize];
		arraySize = new long [initialCliqueSize];
		arrayIndex = new long [initialCliqueSize];
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
		auxiliaryArray = new int [maxVariables];
		clearCliqueTree();
	}
	
	private static long [] expandArray(long [] array, int minNewSize) {
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

	private void ensureSizeOfCliqueArrays(long size) {
		if (summaryValue == null || summaryValue.length < size) {
			if ((size >>> 31)!=0) {
				throw new RuntimeException("I cannot create an array of size: "+size);
			}
			summaryValue = new double [(int)size]; // TODO: Negative array exception
			variableValue = new int [(int)size];
		}
	}

	@Override
	public void applyDynamicProgramming(PBSolution red, EmbeddedLandscape el, List<Integer> [] subFunctionsPartition) {
		ensureQueryState();
		indexAssigner.clearIndex();
		for (int i=numCliques-1; i>=0; i--) {
			prepareStructuresForComputation(i, indexAssigner);
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
		
		long separatorValueLimit = arraySize[clique];
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
			
			summaryValue[(int)(arrayIndex[clique]+separatorValue)]=Double.NEGATIVE_INFINITY; // TODO: Array Index OUt of Bounds
			
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
				if (value > summaryValue[(int)(arrayIndex[clique]+separatorValue)]) {
					summaryValue[(int)(arrayIndex[clique]+separatorValue)] = value;
					variableValue[(int)(arrayIndex[clique] + separatorValue)] = residueValue;
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
			value += summaryValue[(int)(arrayIndex[child] + separatorValue)];
		}
		
		for (int i = variablesOfSeparator[clique]; i < getNumberOfVariablesOfClique(clique); i++) {
			int residueVariable = getVariableOfClique(clique, i);
			for (int fn: subFunctionsPartition[residueVariable]) {
				value += el.evaluateSubFunctionFromCompleteSolution(fn, solution);
			}
		}
		return value;
	}
	
	private void prepareStructuresForComputation(int clique, Function<Long,Long> indexAssignment) {
		int varsInSep = variablesOfSeparator[clique];
		int startIndex = indexOfVariableInClique(clique, 0);
		int endIndex = variableIndex[clique];

		//variableSeparatorLimit[clique] = variableLimitFromList(startIndex, startIndex+varsInSep-1);
		//variableResidueLimit[clique] = variableLimitFromList(startIndex+varsInSep, endIndex);
		
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
				(disjointSets.sameSet(getVariableOfClique(clique, variableSeparatorLimit[clique]),getVariableOfClique(clique,variablesOfSeparator[clique]+variableResidueLimit[clique])));
		
		
		sameGroupsOfNonExploredVariables.set(clique, sameGroup);
	}
	
	private void reconstructSolutionInClique(int clique, PBSolution child, PBSolution red, VariableProcedence variableProcedence, int [] variableValue) {
		int numVariablesOfResidue = getNumberOfVariablesOfClique(clique)-variablesOfSeparator[clique];
		int separatorValue = getSeparatorValueFromSolution(clique, child, red);
		int residueVariables = variableValue[(int)(arrayIndex[clique]+separatorValue)];
		
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
	public void cliqueTreeAnalysis() {
		disjointSets.clear();
		nonExhaustivelyExploredVariables.reset();
		
		for (int varIndex=0; varIndex <= variableIndex[numCliques-1]; varIndex++) {
			disjointSets.makeSet(variables[varIndex]);
			nonExhaustivelyExploredVariables.explored(variables[varIndex]);
		}
		
		int hammingDistance = nonExhaustivelyExploredVariables.getNumberOfExploredElements();
		nonExhaustivelyExploredVariables.reset();
		
		for (int cliqueId=0; cliqueId < numCliques; cliqueId++) {
			int varsInSep = variablesOfSeparator[cliqueId];
			int startIndex = indexOfVariableInClique(cliqueId, 0);
			int endIndex = variableIndex[cliqueId];
			
			fillNonExhaustivelyExploredVariables(startIndex, startIndex+varsInSep-1);
			fillNonExhaustivelyExploredVariables(startIndex+varsInSep, endIndex);
		}
		
		for (int cliqueId=0; cliqueId < numCliques; cliqueId++) {
			int varsInSep = variablesOfSeparator[cliqueId];
			int startIndex = indexOfVariableInClique(cliqueId, 0);
			int endIndex = variableIndex[cliqueId];
			
			variableSeparatorLimit[cliqueId] = analyzeSetOfVariables(startIndex, startIndex+varsInSep-1);
			assert checkExhaustivelyExplored(startIndex, startIndex+varsInSep-1, variableSeparatorLimit[cliqueId]);
			variableResidueLimit[cliqueId] = analyzeSetOfVariables(startIndex+varsInSep, endIndex);
			assert checkExhaustivelyExplored(startIndex+varsInSep, endIndex, variableResidueLimit[cliqueId]);
		}
		
		groupsOfNonExhaustivelyExploredVariables = disjointSets.getNumberOfSets() 
				+ nonExhaustivelyExploredVariables.getNumberOfExploredElements() 
				- hammingDistance;
	}
	
	private int analyzeSetOfVariables(int startIndex, int endIndexInclusive) {
		int nbOfNonExhVariables = 0;
		int toWrite = startIndex;
		int previousVar = -1;
		for (int i=startIndex; i <= endIndexInclusive; i++) {
			int variable = variables[i];
			if (nonExhaustivelyExploredVariables.isExplored(variable)) {
				auxiliaryArray[nbOfNonExhVariables++] = variable;
				if (previousVar >= 0) {
					disjointSets.union(previousVar, variable);
				}
				previousVar = variable;
			} else {
				variables[toWrite++] = variable;
			}
		}
		
		int exhaustivelyExplored = toWrite - startIndex;
		
		for (int i=0; i < nbOfNonExhVariables; i++) {
			variables[toWrite++] = auxiliaryArray[i];
		}
		
		return exhaustivelyExplored;
	}
	
	/**
	 * This method adds an extra check in the code, only used in an assertion
	 * @param startIndex
	 * @param endIndexInclusive
	 * @param exhaustivelyExplored
	 * @return
	 */
	private boolean checkExhaustivelyExplored(int startIndex, int endIndexInclusive, int exhaustivelyExplored) {
		for (int i=startIndex; i < startIndex+exhaustivelyExplored; i++) {
			if (nonExhaustivelyExploredVariables.isExplored(variables[i])) {
				return false;
			}
		}
		
		for (int i=startIndex+exhaustivelyExplored; i <= endIndexInclusive; i++) {
			if (!nonExhaustivelyExploredVariables.isExplored(variables[i])) {
				return false;
			}
		}
		return true;
	}
	
	private void fillNonExhaustivelyExploredVariables(int startIndex, int endIndexInclusive) {
		int variablesToJoin = endIndexInclusive-startIndex+1 - maximumVariablesToExhaustivelyExplore;
		if (variablesToJoin > 0) {
			int nbOfArticulationPoints = 0;
			for (; variablesToJoin > 0 && endIndexInclusive >= startIndex; endIndexInclusive--) {
				int variable = variables[endIndexInclusive];
				if (articulationPoints.isExplored(variable)) {
					auxiliaryArray[nbOfArticulationPoints++] = variable;
				} else {
					nonExhaustivelyExploredVariables.explored(variable);
					variablesToJoin--;
				}
			}
			
			for (int i = 0; variablesToJoin > 0; i++) {
				nonExhaustivelyExploredVariables.explored(auxiliaryArray[i]);
				variablesToJoin--;
			}
		}
	}
	
	@Override
	public boolean allArticulationPointsExhaustivelyExplored() {
		return !nonExhaustivelyExploredVariables.getExplored().anyMatch(articulationPoints::isExplored);
	}

	@Override
	public void addArticulationPoint(int variable) {
		articulationPoints.explored(variable);
	}

	@Override
	public int getNumberOfArticulationPoints() {
		return  articulationPoints.getNumberOfExploredElements();
	}
	
	@Override
	public void setMaximumVariablesToExhaustivelyExplore(int numberOfVariables) {
		maximumVariablesToExhaustivelyExplore = numberOfVariables;
	}

	@Override
	public boolean isDebug() {
		return debug;
	}

	@Override
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public void setDisjointSets(DisjointSets disjointSets) {
		this.disjointSets = disjointSets;
	}
}