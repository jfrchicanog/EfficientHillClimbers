package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Seeds;

public class RBall4MAXSAT extends RBallEfficientHillClimber {

	public RBall4MAXSAT(Properties prop) {
		super(prop);
	}

	public RBall4MAXSAT(int r, long seed) {
		this(r, null, seed);
	}

	public RBall4MAXSAT(int r, double[] quality_l, long seed) {
		super(r, quality_l, seed);
	}

	@Override
	public RBall4MAXSATForInstanceOf initialize(EmbeddedLandscape prob) {
		return new RBall4MAXSATForInstanceOf(this, prob);
	}

}
