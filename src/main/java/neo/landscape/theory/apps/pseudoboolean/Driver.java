package neo.landscape.theory.apps.pseudoboolean;

import java.util.Arrays;
import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class Driver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Something wrong with n=15, k=3 and r=4 (q not set)

		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, "12000");
		prop.setProperty(NKLandscapes.K_STRING, "3");
		// prop.setProperty(NKLandscapes.Q_STRING, "100");
		prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
		int seed = 1;
        pbf.setSeed(seed);

		pbf.setConfiguration(prop);

		int r = 4;
		int limit_moves = 100;

		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r, seed);
		PBSolution pbs = pbf.getRandomSolution();

		long init_time = System.currentTimeMillis();

		RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rball
				.initialize(pbf).initialize(pbs);
		// rball.initialize(pbf, pbs);

		long after_time = System.currentTimeMillis();

		// rball.checkConsistency();
		double init_fitness = pbf.evaluate(pbs);
		assert (init_fitness == rballs.getSolutionQuality());
		double imp = rballs.move();
		// rball.checkConsistency();
		double sum = imp;

		double old_fit, new_fit = init_fitness;
		int j = 0;

		while (imp > 0 && j < limit_moves) {
			old_fit = new_fit;
			new_fit = pbf.evaluate(rballs.getSolution());
			if (new_fit - old_fit != imp) {
				System.out.println("Something wrong (old=" + old_fit + ", new="
						+ new_fit + ", imp=" + imp + " in " + j + ")");
			}
			// System.out.println("Imp:"+imp);
			imp = rballs.move();
			// rball.checkConsistency();
			sum += imp;
			j++;
		}

		double final_fitness = pbf.evaluate(rballs.getSolution());

		assert (final_fitness == rballs.getSolutionQuality());

		long final_time = System.currentTimeMillis();

		System.out.println("Init fitness:" + init_fitness);
		System.out.println("Final fitness:" + final_fitness);
		System.out.println("Improvement:" + sum);
		System.out.println("Initialization time: " + (after_time - init_time));
		System.out.println("Problem initialization time: "
				+ rballs.getHillClimberForInstanceOf().getProblemInitTime());
		System.out.println("Move time: " + (final_time - after_time));
		System.out.println("Total time: " + (final_time - init_time));
		System.out.println("Total moves: " + j);
		System.out.println("Stored scores:"
				+ rballs.getHillClimberForInstanceOf().getStoredScores());
		System.out.println("Moves perdistance:"
				+ Arrays.toString(rballs.getStatistics().getMovesPerDistance()));

		if (sum != final_fitness - init_fitness) {
			System.out.println("Something wrong");
		}

	}

}
