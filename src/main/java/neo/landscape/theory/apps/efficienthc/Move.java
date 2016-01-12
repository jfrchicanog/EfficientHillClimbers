package neo.landscape.theory.apps.efficienthc;

public interface Move<P extends SingleobjectiveProblem, S extends Solution<? super P>> {
	public double getImprovement();

}
