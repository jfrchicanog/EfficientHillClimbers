package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	
	private TwoStatesIntegerSet nonExhaustivelyExploredVariables;
	private int groupsOfNonExhaustivelyExploredVariables;

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
	
	@Override
	public TwoStatesIntegerSet getNonExhaustivelyExploredVariables() {
		return nonExhaustivelyExploredVariables;
	}
	
	@Override
	public void setNonExhaustivelyExploredVariables(TwoStatesIntegerSet nonExhaustivelyExploredVariables) {
		this.nonExhaustivelyExploredVariables = nonExhaustivelyExploredVariables;
	}
	
	@Override
	public int getGroupsOfNonExhaustivelyExploredVariables() {
		return groupsOfNonExhaustivelyExploredVariables;
	}
	
	@Override
	public void setGroupsOfNonExhaustivelyExploredVariables(int groupsOfNonExhaustivelyExploredVariables) {
		this.groupsOfNonExhaustivelyExploredVariables = groupsOfNonExhaustivelyExploredVariables;
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
		setGroupsOfNonExhaustivelyExploredVariables(partitionOfNonExploredVariables.size());
		
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
			listOfVariables.sort(Comparator.<Integer>comparingInt(variable -> dynasticPotentialCrossover.articulationPoints.isExplored(variable)?0:1)
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
		return !getNonExhaustivelyExploredVariables().getExplored().anyMatch(dynasticPotentialCrossover.articulationPoints::isExplored);
	}
}