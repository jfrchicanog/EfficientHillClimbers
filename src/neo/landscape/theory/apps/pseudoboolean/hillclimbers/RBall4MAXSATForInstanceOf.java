package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class RBall4MAXSATForInstanceOf extends
		RBallEfficientHillClimberForInstanceOf {

	public RBall4MAXSATForInstanceOf(RBall4MAXSAT rball,
			EmbeddedLandscape problem) {
		super(rball, problem);
	}

	public RBall4MAXSATSnapshot initialize(
			Solution<? super EmbeddedLandscape> sol) {
		if (sol instanceof PBSolution) {
			return new RBall4MAXSATSnapshot(this, (PBSolution) sol);
		} else {
			throw new IllegalArgumentException(
					"Expected argument of class PBSolution but found "
							+ sol.getClass().getCanonicalName());
		}
	}

}
