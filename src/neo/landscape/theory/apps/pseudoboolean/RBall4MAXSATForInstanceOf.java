package neo.landscape.theory.apps.pseudoboolean;

import neo.landscape.theory.apps.efficienthc.Solution;

public class RBall4MAXSATForInstanceOf extends RBallEfficientHillClimberForInstanceOf {

	public RBall4MAXSATForInstanceOf (RBall4MAXSAT rball, EmbeddedLandscape problem)
	{
		super(rball, problem);
	}
	
	public RBall4MAXSATSnapshot initialize(Solution<? super EmbeddedLandscape> sol) {
		if (sol instanceof PBSolution)
		{
			return new RBall4MAXSATSnapshot(this, (PBSolution)sol);
		}
		else
		{
			throw new IllegalArgumentException ("Expected argument of class PBSolution but found "+sol.getClass().getCanonicalName());
		}
	}
	
}
