package neo.landscape.theory.apps.efficienthc.mo;

import neo.landscape.theory.apps.efficienthc.MultiobjectiveProblem;
import neo.landscape.theory.apps.efficienthc.Solution;

public interface MultiobjectiveMove<P extends MultiobjectiveProblem, S extends Solution<?>> {
	public double [] getImprovement();

}
