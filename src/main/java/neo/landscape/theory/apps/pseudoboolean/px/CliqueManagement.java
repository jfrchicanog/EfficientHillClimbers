package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.List;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.DisjointSets;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public interface CliqueManagement {
	List<VariableClique> getCliques();
	void applyDynamicProgramming(PBSolution red, EmbeddedLandscape el, List<Integer>[] subFunctionsPartition);
	void reconstructOptimalChild(PBSolution child, PBSolution red, VariableProcedence varProcedence);
	void clearCliqueTree();
	VariableClique addNewVariableClique();
	void setVariableCliqueParent (int childID, int parentID);
	public String getCliqueTree();
	void addArticulationPoint(int variable);
	int getNumberOfArticulationPoints();
	void setMaximumVariablesToExhaustivelyExplore(int numberOfVariables);
	void setDebug(boolean debug);
	boolean isDebug();
	void cliqueTreeAnalysis();
	boolean allArticulationPointsExhaustivelyExplored();
	TwoStatesIntegerSet getNonExhaustivelyExploredVariables();
	int getGroupsOfNonExhaustivelyExploredVariables();
	void setDisjointSets(DisjointSets disjointSets);
}