package neo.landscape.theory.apps.efficienthc;

public interface HillClimberSnapshot<P extends Problem> {
	public Move<? super P, ? extends Solution<? super P>> getMovement();
	public double move();
	public <S extends Solution<? super P>> S getSolution();
	public <H extends HillClimberForInstanceOf<P>> H getHillClimberForInstanceOf();
}
