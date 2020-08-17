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
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public interface CliqueManagement {
	List<VariableClique> getCliques();
	void applyDynamicProgramming(TwoStatesIntegerSet nonExhaustivelyExploredVariables, int[] marks, PBSolution red,
			EmbeddedLandscape el, List<Integer>[] subFunctionsPartition);
	void reconstructOptimalChild(PBSolution child, PBSolution red, VariableProcedence varProcedence);
	void clearCliqueTree();
	VariableClique addNewVariableClique();
	void setVariableCliqueParent (int childID, int parentID);
	public String getCliqueTree();
	/**
	 * This method analyzes the clique tree and put limit to the number of variables for which all the combinations are tested.
	 * @param dynasticPotentialCrossover TODO
	 */
	default void cliqueTreeAnalysis(DynasticPotentialCrossover dynasticPotentialCrossover) {
		dynasticPotentialCrossover.cliqueManagement.getNonExhaustivelyExploredVariables(dynasticPotentialCrossover).reset();
		for (VariableClique clique: getCliques()) {
			assert dynasticPotentialCrossover.cliqueManagement.orderOfVariablesInClique(dynasticPotentialCrossover, clique.getVariables());
			
			List<Integer> listOfVariables = clique.getVariables().subList(0,clique.getVariablesOfSeparator());
			dynasticPotentialCrossover.cliqueManagement.checkExplorationLimits(dynasticPotentialCrossover, listOfVariables);
			
			listOfVariables = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			dynasticPotentialCrossover.cliqueManagement.checkExplorationLimits(dynasticPotentialCrossover, listOfVariables);
		}
		
		dynasticPotentialCrossover.cliqueManagement.analysisOfGroupsOfNonExhaustivelyExploredVariables(dynasticPotentialCrossover);
		
	}
	default void analysisOfGroupsOfNonExhaustivelyExploredVariables(DynasticPotentialCrossover dynasticPotentialCrossover) {
		List<Set<Integer>> partitionOfNonExploredVariables = new ArrayList<>();
		for (VariableClique clique: getCliques()) {
			List<Integer> separator = clique.getVariables().subList(0, clique.getVariablesOfSeparator());
			List<Integer> residue = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			for (List<Integer> listOfVariables : new List[] { separator, residue }) {
				Set<Integer> newComponent = listOfVariables.stream().filter(dynasticPotentialCrossover.cliqueManagement.getNonExhaustivelyExploredVariables(dynasticPotentialCrossover)::isExplored)
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
		dynasticPotentialCrossover.cliqueManagement.setGroupsOfNonExhaustivelyExploredVariables(dynasticPotentialCrossover, partitionOfNonExploredVariables.size());
		
		if (dynasticPotentialCrossover.debug) {
			Set<Integer> auxiliar = new HashSet<>();
			for (Set<Integer> component: partitionOfNonExploredVariables) {
				assert auxiliar.stream().filter(component::contains).count()==0;
				auxiliar.addAll(component);
			}
			assert auxiliar.equals(dynasticPotentialCrossover.cliqueManagement.getNonExhaustivelyExploredVariables(dynasticPotentialCrossover).getExplored().boxed().collect(Collectors.toSet()));
		}
		
		for (int i=0; i < partitionOfNonExploredVariables.size(); i++) {
			Set<Integer> component = partitionOfNonExploredVariables.get(i);
			for (int variable : component) {
				dynasticPotentialCrossover.marks[variable] = i;
			}
		}
	}
	default void checkExplorationLimits(DynasticPotentialCrossover dynasticPotentialCrossover, List<Integer> listOfVariables) {
		if (listOfVariables.size() > dynasticPotentialCrossover.maximumNumberOfVariableToExploreExhaustively) {
			listOfVariables.sort(Comparator.<Integer>comparingInt(variable -> dynasticPotentialCrossover.articulationPoints.isExplored(variable)?0:1)
					.thenComparing(Comparator.<Integer>naturalOrder()));
			listOfVariables.subList(dynasticPotentialCrossover.maximumNumberOfVariableToExploreExhaustively, listOfVariables.size())
			.forEach(dynasticPotentialCrossover.cliqueManagement.getNonExhaustivelyExploredVariables(dynasticPotentialCrossover)::explored);
			
		}
	}
	default boolean orderOfVariablesInClique(DynasticPotentialCrossover dynasticPotentialCrossover, List<Integer> list) {
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
	default void setNonExhaustivelyExploredVariables(DynasticPotentialCrossover dynasticPotentialCrossover, TwoStatesIntegerSet nonExhaustivelyExploredVariables) {
		dynasticPotentialCrossover.nonExhaustivelyExploredVariables = nonExhaustivelyExploredVariables;
	}
	default void setGroupsOfNonExhaustivelyExploredVariables(DynasticPotentialCrossover dynasticPotentialCrossover, int groupsOfNonExhaustivelyExploredVariables) {
		dynasticPotentialCrossover.groupsOfNonExhaustivelyExploredVariables = groupsOfNonExhaustivelyExploredVariables;
	}
	default boolean allArticulationPointsExhaustivelyExplored(DynasticPotentialCrossover dynasticPotentialCrossover) {
		return !dynasticPotentialCrossover.cliqueManagement.getNonExhaustivelyExploredVariables(dynasticPotentialCrossover).getExplored().anyMatch(dynasticPotentialCrossover.articulationPoints::isExplored);
	}
	default int getGroupsOfNonExhaustivelyExploredVariables(DynasticPotentialCrossover dynasticPotentialCrossover) {
		return dynasticPotentialCrossover.groupsOfNonExhaustivelyExploredVariables;
	}
	default TwoStatesIntegerSet getNonExhaustivelyExploredVariables(DynasticPotentialCrossover dynasticPotentialCrossover) {
		return dynasticPotentialCrossover.nonExhaustivelyExploredVariables;
	}

}