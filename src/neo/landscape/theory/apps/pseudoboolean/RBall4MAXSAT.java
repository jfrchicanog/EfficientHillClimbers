package neo.landscape.theory.apps.pseudoboolean;

import java.util.Properties;

public class RBall4MAXSAT extends RBallEfficientHillClimber {

	public RBall4MAXSAT(Properties prop) {
		super(prop);
	}

	public RBall4MAXSAT(int r) {
		this(r, null);
	}

	public RBall4MAXSAT(int r, double[] quality_l) {
		super(r, quality_l);
	}

	@Override
	public RBall4MAXSATForInstanceOf initialize(EmbeddedLandscape prob) {
		return new RBall4MAXSATForInstanceOf(this, prob);
	}

}
