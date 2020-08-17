package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.DisjointSets;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableCliqueImplementation;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class CliqueManagementBasicImplementation implements CliqueManagement {	
	public static final CliqueManagementFactory FACTORY = new CliqueManagementFactory() {
		@Override
		public CliqueManagement createCliqueManagement(int maxVariables) {
			return new CliqueManagementBasicImplementation(maxVariables);
		}
	};

	private List<VariableClique> cliques;
	private double [] summaryValue;
	private int [] variableValue;
	private final IndexAssigner indexAssigner = new IndexAssigner();
	private int numCliques;
	
	private TwoStatesIntegerSet nonExhaustivelyExploredVariables;
	private int groupsOfNonExhaustivelyExploredVariables;
	private TwoStatesIntegerSet articulationPoints;
	private int maximumVariablesToExhaustivelyExplore;
	
	private boolean debug;
	private DisjointSets disjointSets;

	private CliqueManagementBasicImplementation(int maxVariables) {
		cliques = new ArrayList<>();
		nonExhaustivelyExploredVariables = new TwoStatesISArrayImpl(maxVariables);
		articulationPoints = new TwoStatesISArrayImpl(maxVariables);
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
	public void applyDynamicProgramming(PBSolution red, EmbeddedLandscape el, List<Integer> [] subFunctionsPartition) {
		indexAssigner.clearIndex();
		for (int i=numCliques-1; i>=0; i--) {
			cliques.get(i).prepareStructuresForComputation(nonExhaustivelyExploredVariables, disjointSets, indexAssigner);
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
		groupsOfNonExhaustivelyExploredVariables=0;
		nonExhaustivelyExploredVariables.reset();
		articulationPoints.reset();
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
		nonExhaustivelyExploredVariables.reset();
		for (VariableClique clique: cliques) {
			
			List<Integer> listOfVariables = clique.getVariables().subList(0,clique.getVariablesOfSeparator());
			checkExplorationLimits(listOfVariables);
			
			listOfVariables = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			checkExplorationLimits(listOfVariables);
		}
		
		analysisOfGroupsOfNonExhaustivelyExploredVariables();
		
	}
	private void analysisOfGroupsOfNonExhaustivelyExploredVariables() {
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
		
		if (debug) {
			Set<Integer> auxiliar = new HashSet<>();
			for (Set<Integer> component: partitionOfNonExploredVariables) {
				assert auxiliar.stream().filter(component::contains).count()==0;
				auxiliar.addAll(component);
			}
			assert auxiliar.equals(getNonExhaustivelyExploredVariables().getExplored().boxed().collect(Collectors.toSet()));
		}
		
		for (int i=0; i < partitionOfNonExploredVariables.size(); i++) {
			Set<Integer> component = partitionOfNonExploredVariables.get(i);
			int previousVar = -1;
			for (int variable : component) {
				disjointSets.makeSet(variable);
				if (previousVar >= 0) {
					disjointSets.union(previousVar, variable);
				}
				previousVar = variable;
			}
		}
	}
	private void checkExplorationLimits(List<Integer> listOfVariables) {
		if (listOfVariables.size() > maximumVariablesToExhaustivelyExplore) {
			listOfVariables.sort(Comparator.<Integer>comparingInt(variable -> articulationPoints.isExplored(variable)?0:1)
					.thenComparing(Comparator.<Integer>naturalOrder()));
			listOfVariables.subList(maximumVariablesToExhaustivelyExplore, listOfVariables.size())
			.forEach(getNonExhaustivelyExploredVariables()::explored);
			
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