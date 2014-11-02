package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import neo.landscape.theory.apps.pseudoboolean.Process;
import neo.landscape.theory.apps.pseudoboolean.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBall4MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.util.Seeds;

public class MaxSATExperiment implements Process {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return "maxsat";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: "
				+ getID()
				+ " [-h] [-i <instance>] [-r <r>] [-ma(xsat specific HC)] [-l <quality> <limits> ...] [-t <time(s)>] [-d <descents>] [-s <soft_restart>] [-se <seed>] [-tr(ace)] [-de(bug)] [-fl(ips) vs appearance] [-hp(hyperplane initialization, only for cnf instances)]";
	}

	private boolean checkOneValue(Map<String, List<String>> options,
			String key, String name) {
		if (!options.containsKey(key) || options.get(key).size() == 0) {
			System.err.println("Error: " + name + " missing");
			return false;
		} else if (options.get(key).size() > 1) {
			System.err.println("Error: Multiple " + name + ": "
					+ options.get(key));
			return false;
		}
		return true;
	}

	private void showMaxsatHelp() {
		System.out.println(getInvocationInfo());
	}

	private void checkQualityLimits(double[] res) {
		if (res == null)
			return;

		Arrays.sort(res);

		for (int i = 1; i < res.length; i++) {
			if (res[i] == res[i - 1]) {
				throw new IllegalArgumentException(
						"Repeated value for the quality limits: " + res[i]);
			}
		}
	}

	@Override
	public void execute(String[] args) {
		if (args.length == 0) {
			showMaxsatHelp();
			return;
		}

		// else

		Map<String, List<String>> options = UtilityMethods
				.optionsProcessing(args);

		if (options.containsKey("h")) {
			showMaxsatHelp();
			return;
		}

		// else

		// Check mandatory elements
		// Check instance
		if (!checkOneValue(options, "i", "instance file"))
			return;
		// else
		String instance = options.get("i").get(0);

		// Check the radius
		if (!checkOneValue(options, "r", "radius"))
			return;
		// else
		int r = Integer.parseInt(options.get("r").get(0));

		// Check the stopping criterion
		if (!(options.containsKey("t") || options.containsKey("d"))) {
			System.err.println("Error: stopping condition not specified");
			return;
		}

		long stopDescents = Long.MAX_VALUE;
		long stopTime = Long.MAX_VALUE;

		if (options.containsKey("t")) {
			if (!checkOneValue(options, "t", "stop time"))
				return;
			// else
			stopTime = Long.parseLong(options.get("t").get(0)) * 1000;
		} else {
			if (!checkOneValue(options, "d", "max descents"))
				return;
			// else
			stopDescents = Long.parseLong(options.get("d").get(0));
		}

		// Check optional elements

		// Check quality limits
		double[] qualityLimits = null;
		if (options.containsKey("l") && options.get("l").size() > 0) {
			qualityLimits = new double[options.get("l").size()];
			int i = 0;
			for (String val : options.get("l")) {
				qualityLimits[i++] = Double.parseDouble(val);
			}
		}
		checkQualityLimits(qualityLimits);

		// Check seed
		long seed;
		if (options.containsKey("se")) {
			if (!checkOneValue(options, "se", "seed"))
				return;
			seed = Long.parseLong(options.get("se").get(0));
		} else {
			seed = Seeds.getSeed();
			;
		}

		// Check debug
		boolean debug = options.containsKey("de");

		// Check trace
		boolean trace = options.containsKey("tr");

		// Check maxsat specific
		boolean maxsatSpec = options.containsKey("ma");

		// Check flips vs appearance plot
		boolean flipvsapp = options.containsKey("fl");

		// Check if we should use the hyperplane initialization
		boolean hpInit = options.containsKey("hp");

		// Create the problem

		MAXSAT pbf = new MAXSAT();
		Properties prop = new Properties();
		prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
		prop.setProperty(MAXSAT.HYPERPLANE_INIT, hpInit ? "yes" : "no");

		pbf.setSeed(seed);
		pbf.setConfiguration(prop);

		// Check the soft restart
		int softRestart = -1;
		if (options.containsKey("s")) {
			if (!checkOneValue(options, "s", "soft restart fraction"))
				return;
			double sr = Double.parseDouble(options.get("s").get(0));
			if (sr > 0) {
				softRestart = (int) (pbf.getN() * sr);
				if (softRestart <= 0) {
					softRestart = -1;
				} else if (softRestart > pbf.getN()) {
					softRestart = pbf.getN();
				}
			}
		}

		// Prepare the output
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream(new GZIPOutputStream(ba));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Prepare the hill climber

		Properties rballProperties = new Properties();
		rballProperties.setProperty(RBallEfficientHillClimber.R_STRING,
				String.valueOf(r));
		rballProperties.setProperty(RBallEfficientHillClimber.SEED,
				String.valueOf(seed));
		if (flipvsapp) {
			rballProperties.setProperty(RBallEfficientHillClimber.FLIP_STAT, "");
		}
		if (qualityLimits != null) {
			String str = "" + qualityLimits[0];
			for (int i = 1; i < qualityLimits.length; i++) {
				str += " " + qualityLimits[i];
			}
			rballProperties.setProperty(RBallEfficientHillClimber.QUALITY_LIMITS,
					str);
		}

		RBallEfficientHillClimber rball;
		if (maxsatSpec) {
			rball = new RBall4MAXSAT(rballProperties);
		} else {
			rball = new RBallEfficientHillClimber(rballProperties);
		}

		RBallEfficientHillClimberForInstanceOf rballfio = (RBallEfficientHillClimberForInstanceOf) rball
				.initialize(pbf);
		RBallEfficientHillClimberSnapshot rballs = null;

		// Initialize data
		long initTime = System.currentTimeMillis();
		long elapsedTime = initTime;
		double solutionQuality = -Double.MAX_VALUE;
		boolean firstTime = true;
		PBSolution bestSolution = null;
		long bestSolutionTime = -1;
		long descents = 0;

		while (elapsedTime - initTime < stopTime && descents < stopDescents) {
			if (softRestart < 0 || firstTime) {
				PBSolution pbs = pbf.getRandomSolution();
				rballs = rballfio.initialize(pbs);
				firstTime = false;
			} else {
				rballs.softRestart(softRestart);
			}

			double initialQuality = rballs.getSolutionQuality();
			double imp;
			long moves = 0;

			rballs.resetMovesPerDistance();

			do {
				imp = rballs.move();
				if (debug)
					rballs.checkConsistency();
				moves++;
			} while (imp > 0);
			moves--;

			double finalQuality = rballs.getSolutionQuality();

			descents++;
			elapsedTime = System.currentTimeMillis();

			if (finalQuality > solutionQuality) {
				solutionQuality = finalQuality;
				bestSolution = new PBSolution(rballs.getSolution());
				bestSolutionTime = elapsedTime;
			}

			if (trace) {
				ps.println("Moves: " + moves);
				ps.println("Move histogram: "
						+ Arrays.toString(rballs.getMovesPerDinstance()));
				ps.println("Improvement: " + (finalQuality - initialQuality));
				ps.println("Best solution quality: " + solutionQuality);
				ps.println("Elapsed Time: " + (elapsedTime - initTime));
			}

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

		ps.println("Solution: " + bestSolution);
		ps.println("Quality: " + (solutionQuality + pbf.getTopClauses()));
		ps.println("Time: " + (bestSolutionTime - initTime));
		ps.println("Descents: " + descents);
		ps.println("N: " + pbf.getN());
		ps.println("M: " + pbf.getM());
		ps.println("R: " + r);
		ps.println("Top clauses: " + pbf.getTopClauses());
		ps.println("Seed: " + seed);
		ps.println("Stored scores:" + rballfio.getStoredScores());
		ps.println("Var appearance (histogram):" + appearance);
		ps.println("Var interaction (histogram):" + interactions);

		if (flipvsapp) {
			int[] flips = rballs.getFlipStat();
			int[][] appearsIn = pbf.getAppearsIn();
			// Show the flip vs appearance data (CSV values)
			ps.println("Flips vs appearance: CSV data below");
			ps.println("flips,appearance");
			for (int i = 0; i < flips.length; i++) {
				ps.println(flips[i] + "," + appearsIn[i].length);
			}
			ps.println("CSV End");
		}

		ps.close();

		try {
			System.out.write(ba.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
