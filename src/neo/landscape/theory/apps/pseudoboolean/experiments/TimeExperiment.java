package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.util.Properties;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class TimeExperiment implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "time";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID()
				+ " <n> <k> <q> <circular> <r> <moves> [<seed>]";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 6) {
			System.out.println(getInvocationInfo());
			return;
		}

		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		int r = Integer.parseInt(args[4]);
		int moves = Integer.parseInt(args[5]);
		long seed = 0;
		if (args.length >= 7) {
			seed = Long.parseLong(args[6]);
		} else {
			seed = Seeds.getSeed();
		}

		NKLandscapes pbf = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);

		if (!q.equals("-")) {
			prop.setProperty(NKLandscapes.Q_STRING, q);
		}

		if (circular.equals("y")) {
			prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
		}

		pbf.setSeed(seed);
		pbf.setConfiguration(prop);

		RBallEfficientHillClimber rball = new RBallEfficientHillClimber(r);
		PBSolution pbs = pbf.getRandomSolution();

		/*
		 * long all_sols = System.currentTimeMillis(); for (int t=0; t < 100;
		 * t++) { rball.initialize(pbf, pbf.getRandomSolution()); }
		 */
		RBallEfficientHillClimberSnapshot rballs = (RBallEfficientHillClimberSnapshot) rball
				.initialize(pbf).initialize(pbs);

		// long total_all_sols = System.currentTimeMillis()-all_sols;

		int n_int = pbf.getN();
		int bit = 0;
		long init_time = System.currentTimeMillis();

		for (int m = 0; m < moves; m++) {
			rballs.moveOneBit(bit++);
			if (bit == n_int) {
				bit = 0;
			}
		}

		long final_time = System.currentTimeMillis();

		int max_app = 0;
		int max_interactions = 0;
		for (int i = 0; i < n_int; i++) {
			if (pbf.getAppearsIn()[i].length > max_app) {
				max_app = pbf.getAppearsIn()[i].length;
			}

			if (pbf.getInteractions()[i].length > max_interactions) {
				max_interactions = pbf.getInteractions()[i].length;
			}
		}

		System.out.println("Problem init time: "
				+ rballs.getHillClimberForInstanceOf().getProblemInitTime());
		System.out.println("Solution init time: "
				+ rballs.getSolutionInitTime());
		System.out.println("Move time: " + (final_time - init_time));
		System.out.println("Stored scores:"
				+ rballs.getHillClimberForInstanceOf().getStoredScores());
		// System.out.println("Total solution init time: "+total_all_sols);
		// System.out.println("Total moves: "+rball.getTotalMoves());
		// System.out.println("Subfns evals in moves: "+rball.getSubfnsEvalsInMoves());
		// System.out.println("Total solution inits: "+rball.getTotalSolutionInits());
		// System.out.println("Subfns evals in sol inits: "+rball.getSubfnsEvalsInSolInits());
		System.out.println("Var appearance (max):" + max_app);
		System.out.println("Var interaction (max):" + max_interactions);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
