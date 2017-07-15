package neo.landscape.theory.apps.efficienthc.mo;

import neo.landscape.theory.apps.efficienthc.MultiobjectiveProblem;

public interface MultiobjectiveHillClimber<P extends MultiobjectiveProblem> {

	public MultiobjectiveHillClimberForInstanceOf<P> initialize(P prob);
	/*
	 * public Move<? super P, ? extends Solution<? super P>> getMovement();
	 * public double move(); public <S extends Solution<? super P>> S
	 * getSolution();
	 */
}
