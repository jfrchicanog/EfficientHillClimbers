package neo.landscape.theory.apps.efficienthc;

public interface HillClimberForInstanceOf<P extends Problem> {
	public HillClimberSnapshot<P>  initialize(Solution<? super P> sol);
	public <H extends HillClimber<P>> H getHillClimber();
}
