package neo.landscape.theory.apps.efficienthc;

public interface HillClimber<P extends Problem> {

	public void initialize(P prob, Solution<? super P> sol);
	public Move<? super P, ? extends Solution<? super P>> getMovement();
	public double move();
	public <S extends Solution<? super P>> S getSolution();
}
