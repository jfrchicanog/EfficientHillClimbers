package neo.landscape.theory.apps.efficienthc;

public interface HillClimber<P extends SingleobjectiveProblem> {

	public HillClimberForInstanceOf<P> initialize(P prob);
	/*
	 * public Move<? super P, ? extends Solution<? super P>> getMovement();
	 * public double move(); public <S extends Solution<? super P>> S
	 * getSolution();
	 */
}
