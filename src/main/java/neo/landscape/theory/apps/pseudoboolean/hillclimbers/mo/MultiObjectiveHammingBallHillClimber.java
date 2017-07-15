package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

import java.util.Properties;
import java.util.Random;

import neo.landscape.theory.apps.efficienthc.mo.MultiobjectiveHillClimber;
import neo.landscape.theory.apps.efficienthc.mo.MultiobjectiveHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.util.Seeds;

public class MultiObjectiveHammingBallHillClimber implements
		MultiobjectiveHillClimber<VectorMKLandscape> {

	public static final String R_STRING = "r";
	public static final String FLIP_STAT = "flip_stat";
	public static final String SEED = "seed";
	public static final String RANDOM_MOVES = "random";

	protected boolean randomMoves;
	protected int radius;
	protected boolean collectFlips;
	protected Properties configuration;
	protected Random rnd;
	

	public MultiObjectiveHammingBallHillClimber(Properties prop) {
		configuration = (Properties) prop.clone();
		initializeOperator(configuration);
	}

    private void initializeOperator(Properties prop) {
        if (!prop.containsKey(R_STRING)) {
			throw new IllegalArgumentException(
					"Radius of explorarion not found (r)");
		}
        this.radius = Integer.parseInt(prop.getProperty(R_STRING));
		long seed;
		if (prop.containsKey(SEED)) {
			seed = Long.parseLong(prop.getProperty(SEED));
		} else {
			seed = Seeds.getSeed();
		}

		collectFlips = prop.containsKey(FLIP_STAT);
		if (prop.containsKey(RANDOM_MOVES)) {
		    randomMoves = prop.getProperty(RANDOM_MOVES).equals("yes");
		}
		rnd = new Random(seed);
    }

	@Override
	public MultiobjectiveHillClimberForInstanceOf<VectorMKLandscape> initialize(
			VectorMKLandscape prob) {
		return new MultiObjectiveHammingBallHillClimberForInstanceOf(this, prob);
	}

}
