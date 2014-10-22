package neo.landscape.theory.apps.graphcoloring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.util.Seeds;

/**
 * 
 * @author francis
 *
 */

public class Driver {

	public void runDebugExecution(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Arguments: <instance> <colors> [<trials>]");
			return;
		}

		Properties prop = new Properties();
		prop.setProperty("instance", args[0]);
		prop.setProperty("colors", args[1]);

		int trials = 1;

		WeightedGraphColoring wgc = new WeightedGraphColoring();
		wgc.setConfiguration(prop);

		if (args.length > 2) {
			trials = Integer.parseInt(args[2]);
		}

		System.out.println(wgc);

		for (int i = 0; i < trials; i++) {
			WGCSolution sol = wgc.getRandomSolution();
			System.out.println(sol);
			double fitness = wgc.evaluate(sol);
			System.out.println("Fitness:" + fitness);

			File dot = new File("sol-init.dot");
			FileOutputStream fos = new FileOutputStream(dot);
			PrintWriter pw = new PrintWriter(fos);
			pw.println(wgc.dotLanguage(sol));
			pw.close();

			HillClimber<WeightedGraphColoring> hc = new EfficientHillClimber();
			HillClimberSnapshot<WeightedGraphColoring> hcs = hc.initialize(wgc)
					.initialize(sol);

			int l = 0;
			double imp = 0.0;
			do {
				dot = new File("sol-int" + l + ".dot");
				fos = new FileOutputStream(dot);
				pw = new PrintWriter(fos);

				WGCMove m = (WGCMove) hcs.getMovement();
				WGCSolution s = (WGCSolution) hcs.getSolution();
				pw.println(wgc.dotLanguage(s, m));
				pw.close();

				imp = hcs.move();
				System.out.println("Improvement:" + imp);
				l++;

			} while (imp < 0);

			dot = new File("sol-final.dot");
			fos = new FileOutputStream(dot);
			pw = new PrintWriter(fos);
			pw.println(wgc.dotLanguage((WGCSolution) hcs.getSolution()));
			pw.close();

		}
	}

	public void runHillClimbing(String[] args) {
		if (args.length < 4) {
			System.out
					.println("Arguments: <algorithm (naive or efficient)> <instance (file)> <colors(int)> <moves(int)> [<seed>]");
			return;
		}

		String algorithm = args[0];
		String instance = args[1];
		String colors = args[2];
		int moves = Integer.parseInt(args[3]);
		long seed = Seeds.getSeed();

		if (args.length >= 5) {
			seed = Long.parseLong(args[4]);
		}

		HillClimber<WeightedGraphColoring> hc = createHillClimber(algorithm);

		Properties prop = new Properties();
		prop.setProperty("instance", instance);
		prop.setProperty("colors", colors);

		WeightedGraphColoring wgc = new WeightedGraphColoring();
		wgc.setConfiguration(prop);
		wgc.setSeed(seed);

		HillClimberForInstanceOf<WeightedGraphColoring> hcfio = hc
				.initialize(wgc);

		int n_moves = 0;
		long init_time = System.currentTimeMillis();
		double best_sol = Double.MAX_VALUE;
		int restarts = 0;

		do {
			HillClimberSnapshot<WeightedGraphColoring> hcs = hcfio
					.initialize(wgc.getRandomSolution());
			restarts++;
			double imp = 0.0;
			do {
				imp = hcs.move();
				if (imp < 0.0) {
					n_moves++;
				}
			} while (n_moves < moves && imp < 0.0);

			double fitness = wgc.evaluate(hcs.getSolution());
			if (fitness < best_sol) {
				best_sol = fitness;
			}

		} while (n_moves < moves);
		long stop_time = System.currentTimeMillis();

		System.out.println("Time (ms): " + (stop_time - init_time));
		System.out.println("Best solution: " + best_sol);
		System.out.println("Restarts: " + restarts);
		System.out.println("Vertices: " + wgc.getVertices());
		System.out.println("Edges: " + wgc.getEdges());
		System.out.println("Colors: " + wgc.getColors());
		System.out.println("Max degree:" + wgc.getMaxDegree());

	}

	private HillClimber<WeightedGraphColoring> createHillClimber(String name) {
		if (name.equals("naive")) {
			return new NaiveHillClimber();
		} else if (name.equals("efficient")) {
			return new EfficientHillClimber();
		} else {
			throw new RuntimeException("Algorithm " + name
					+ " is not a recognized algorithm.");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Driver d = new Driver();

		// d.runDebugExecution(args);
		d.runHillClimbing(args);

	}

}
