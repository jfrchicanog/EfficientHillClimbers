package neo.landscape.theory.apps.efficienthc.mo;

import neo.landscape.theory.apps.efficienthc.MultiobjectiveProblem;
import neo.landscape.theory.apps.efficienthc.Solution;

public interface MultiobjectiveHillClimberForInstanceOf<P extends MultiobjectiveProblem> {
	public MultiobjectiveHillClimberSnapshot<P> initialize(double [] weights, Solution<?> sol);
	public <H extends MultiobjectiveHillClimber<P>> H getHillClimber();
	public P getProblem();
}
