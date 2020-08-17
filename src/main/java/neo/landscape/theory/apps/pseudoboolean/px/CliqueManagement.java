package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.List;

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
	void addArticulationPoint(int variable);
	int getNumberOfArticulationPoints();
	/**
	 * This method analyzes the clique tree and put limit to the number of variables for which all the combinations are tested.
	 * @param dynasticPotentialCrossover TODO
	 */
	void cliqueTreeAnalysis(DynasticPotentialCrossover dynasticPotentialCrossover);
	boolean allArticulationPointsExhaustivelyExplored(DynasticPotentialCrossover dynasticPotentialCrossover);
	TwoStatesIntegerSet getNonExhaustivelyExploredVariables();
	int getGroupsOfNonExhaustivelyExploredVariables();

}