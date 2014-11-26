package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Properties;
import java.util.Random;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Seeds;

public class RBallEfficientHillClimber implements
		HillClimber<EmbeddedLandscape> {

	public static final String R_STRING = "r";
	public static final String FLIP_STAT = "flip_stat";
	public static final String QUALITY_LIMITS = "ql";
	public static final String SEED = "seed";
	public static final String FIFO = "fifo";

	/* Operator dependent structures */

	// Main configuration parameters and variables
	/* Operator info */
	protected double[] quality_limits;
	/* Operator info */
	protected boolean fifo;
	/* Operator info */
	protected int radius;
	protected boolean collect_flips;
	protected Properties configuration;
	protected Random rnd;

	public RBallEfficientHillClimber(Properties prop) {
		configuration = (Properties) prop.clone();

		if (!prop.containsKey(R_STRING)) {
			throw new IllegalArgumentException(
					"Radius of explorarion not found (r)");
		}
		int r = Integer.parseInt(prop.getProperty(R_STRING));
		double[] quality_l = parseQL(prop.getProperty(QUALITY_LIMITS));
		long seed;
		if (prop.containsKey(SEED)) {
			seed = Long.parseLong(prop.getProperty(SEED));
		} else {
			seed = Seeds.getSeed();
		}

		collect_flips = prop.containsKey(FLIP_STAT);
		fifo = prop.containsKey(FIFO);

		quality_limits = quality_l;
		this.radius = r;
		rnd = new Random(seed);
	}

	/* Operator method */
	private double[] parseQL(String s) {
		if (s == null) {
			return null;
		}
		String[] strs = s.split(" ");
		double[] res = new double[strs.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = Double.parseDouble(strs[i]);
		}
		return res;

	}

	public RBallEfficientHillClimber(int r) {
		// Kept for backward compatibility
		this(r, null);
	}

	public RBallEfficientHillClimber(int r, double[] quality_l) {
		// Kept for backward compatibility
		quality_limits = quality_l;
		this.radius = r;
		rnd = new Random(Seeds.getSeed());
		collect_flips = false;
		fifo = false;
		configuration = new Properties();
		configuration.setProperty(R_STRING, "" + radius);
	}

	protected int getQualityIndex(double val) {
		if (val <= 0) {
			return 0;
		} else if (quality_limits == null) {
			return 1;
		} else {
			int i = 0;
			for (i = 0; i < quality_limits.length && quality_limits[i] <= val; i++)
				;
			return i + 1;
		}
	}

	@Override
	public HillClimberForInstanceOf<EmbeddedLandscape> initialize(
			EmbeddedLandscape prob) {
		return new RBallEfficientHillClimberForInstanceOf(this, prob);
	}

}
