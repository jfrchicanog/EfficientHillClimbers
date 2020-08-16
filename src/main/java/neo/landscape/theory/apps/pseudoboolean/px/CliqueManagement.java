package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.List;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;

public interface CliqueManagement {
	List<VariableClique> getCliques();
	void applyDynamicProgramming(Set<Integer> nonExhaustivelyExploredVariables, int[] marks, PBSolution red,
			EmbeddedLandscape el, List<Integer>[] subFunctionsPartition);
	void reconstructOptimalChild(PBSolution child, PBSolution red, VariableProcedence varProcedence);
	void clearCliqueTree();
	VariableClique addNewVariableClique();
	void setVariableCliqueParent (int childID, int parentID);

}