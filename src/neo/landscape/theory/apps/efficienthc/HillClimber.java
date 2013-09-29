package neo.landscape.theory.apps.efficienthc;

public interface HillClimber<P extends Problem> {

	public void initialize(P prob, Solution<P> sol);
	public Move<P, ?> getMovement();
	public double move();
	public <S extends Solution<P>> S getSolution();
}
