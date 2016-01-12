package neo.landscape.theory.apps.efficienthc.mo;

import neo.landscape.theory.apps.efficienthc.MultiobjectiveProblem;
import neo.landscape.theory.apps.efficienthc.Solution;

public interface MultiobjectiveHillClimberSnapshot<P extends MultiobjectiveProblem> {
	public MultiobjectiveMove<? super P, ? extends Solution<?>> getMovement();
	public double [] move();
	public <S extends Solution<? super P>> S getSolution();
	public <H extends MultiobjectiveHillClimberForInstanceOf<P>> H getHillClimberForInstanceOf();
}
