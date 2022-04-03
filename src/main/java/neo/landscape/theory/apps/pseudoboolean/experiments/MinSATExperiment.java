package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class MinSATExperiment implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "minsat";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: minsat <instance> <r> <time(s)> [<seed>]";
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 3) {
			System.out.println(getInvocationInfo());
			return;
		}

		String instance = args[0];
		int r = Integer.parseInt(args[1]);
		long time = Integer.parseInt(args[2]);
		long seed = 0;
		if (args.length >= 4) {
			seed = Long.parseLong(args[3]);
		} else {
			seed = Seeds.getSeed();
		}

		MAXSAT pbf = new MAXSAT();

		Properties prop = new Properties();
		prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
		prop.setProperty(MAXSAT.MIN_STRING, "yes");

		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream(new GZIPOutputStream(ba));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		pbf.setSeed(seed);
		pbf.setConfiguration(prop);

		RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(
				r).initialize(pbf);

		long initTime = System.currentTimeMillis();
		long elapsedTime = initTime;
		double solutionQuality = -Double.MAX_VALUE;

		while (elapsedTime - initTime < time * 1000) {
			PBSolution pbs = pbf.getRandomSolution();
			RBallEfficientHillClimberSnapshot rball = rballfio.initialize(pbs);
			double initialQuality = rball.getSolutionQuality();
			double imp;
			long moves = 0;

			rball.resetMovesPerDistance();

			do {
				imp = rball.move();
				moves++;
			} while (imp > 0);
			moves--;

			double finalQuality = rball.getSolutionQuality();

			if (finalQuality > solutionQuality) {
				solutionQuality = finalQuality;
			}

			elapsedTime = System.currentTimeMillis();

			ps.println("Moves: " + moves);
			ps.println("Move histogram: "
					+ Arrays.toString(rball.getMovesPerDinstance()));
			ps.println("Improvement: " + (finalQuality - initialQuality));
			ps.println("Best solution quality: " + solutionQuality);
			ps.println("Elapsed Time: " + (elapsedTime - initTime));

		}

		Map<Integer, Integer> appearance = new HashMap<Integer, Integer>();
		Map<Integer, Integer> interactions = new HashMap<Integer, Integer>();

		for (int i = 0; i < pbf.getN(); i++) {
			int appears = pbf.getAppearsIn()[i].length;
			if (appearance.get(appears) == null) {
				appearance.put(appears, 1);
			} else {
				appearance.put(appears, appearance.get(appears) + 1);
			}

			int interacts = pbf.getInteractions()[i].length;
			if (interactions.get(interacts) == null) {
				interactions.put(interacts, 1);
			} else {
				interactions.put(interacts, interactions.get(interacts) + 1);
			}

		}

		ps.println("N: " + pbf.getN());
		ps.println("M: " + pbf.getM());
		ps.println("R: " + r);
		ps.println("Top clauses: " + pbf.getTopClauses());
		ps.println("Seed: " + seed);
		ps.println("Stored scores:" + rballfio.getStoredScores());
		ps.println("Var appearance (histogram):" + appearance);
		ps.println("Var interaction (histogram):" + interactions);

		ps.close();

		try {
			System.out.write(ba.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}