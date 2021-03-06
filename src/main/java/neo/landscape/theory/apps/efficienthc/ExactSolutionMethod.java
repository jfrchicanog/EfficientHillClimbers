package neo.landscape.theory.apps.efficienthc;

public interface ExactSolutionMethod<P extends SingleobjectiveProblem> {
	public static final class SolutionQuality<P> {
		public Solution<? super P> solution;
		public double quality;
	}

	public SolutionQuality<P> solveProblem(P problem);

}
