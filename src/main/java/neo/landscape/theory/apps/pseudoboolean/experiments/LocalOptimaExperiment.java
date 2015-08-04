package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossover;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class LocalOptimaExperiment implements Process {

	private class SolutionFrequency {
		public PBSolution solution;
		public double fitness;
		public int frequency;

		public SolutionFrequency(PBSolution s, int freq) {
			this.solution = s;
			this.frequency = freq;
			fitness = pbf.evaluate(solution);
		}

	}

	private List<PBSolution> localOptima;
	private PrintWriter nodesFile;
	private PrintWriter edgesFile;
	private PrintWriter histogramFile;
	private PrintWriter gpProgram;
	private Set<Integer> appearedEdges;

	private int[] localOptimaHistogram;

	private NKLandscapes pbf;
	private int r;
    private RBallEfficientHillClimberSnapshot rball;
    private long initTime;
    private long finalTime;
    private int max_app;
    private int max_interactions;
    private long timeAfterCrossover;
    private RBallEfficientHillClimberForInstanceOf rballfio;
    private long seed;

	public LocalOptimaExperiment() {
		localOptima = new ArrayList<PBSolution>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the All the Local Optima of the search "
				+ "space using the Efficient RBall exploration algorithm";
	}

	@Override
	public String getID() {
		return "lo";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + " <n> <k> <q> <circular> <r> [<seed>]";
	}

	private void notifyLocalOptima(RBallEfficientHillClimberSnapshot rball,
			NKLandscapes pbf) {
		double imp = rball.getMovement().getImprovement();
		if (imp <= 0.0) {
			PBSolution lo = new PBSolution(rball.getSolution());
			double val = pbf.evaluate(lo);
			System.out.println(wI(localOptima.size()) + ": " + lo + ": " + val);
			localOptima.add(lo);
			nodesFile.println(val);
		}
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 5) {
			System.out.println(getInvocationInfo());
			return;
		}

		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		r = Integer.parseInt(args[4]);
		seed = 0;
		if (args.length >= 6) {
			seed = Long.parseLong(args[5]);
		} else {
			seed = Seeds.getSeed();
		}

		prepareEdgesList();
		
		createInstance(n, k, q, circular);
        String file_name = computeFileName(n, k, q, circular);
        
		createAndOpenOutputFiles(file_name);

		prepareRBallExplorationAlgorithm();

		localOptima=findLocalOptima();
		
		localOptimaHistogram = createLocalOptimaHistogram(localOptima);
		applyPartitionCrossoverToAllPairsOfLocalOptima();

		writeHistogram(localOptimaHistogram);
		writeGNUPlotProgram(file_name);

		computeVariablesStatistics();

		reportStatistics();
		reportInstanceToStandardOutput();
		closeOutputFiles();

	}

    private String computeFileName(String n, String k, String q, String circular) {
        return "nkq-" + n + "-" + k + "-" + q + "-" + circular
                + "-" + r + "-" + seed;
    }

    private void prepareEdgesList() {
        appearedEdges = new HashSet<Integer>();
    }

    private int[] createLocalOptimaHistogram(List<PBSolution> localOptima) {
        localOptimaHistogram = new int[localOptima.size()];
        return localOptimaHistogram;
    }

    private void createInstance(String n, String k, String q, String circular) {
        pbf = new NKLandscapes();
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
    }

    private void prepareRBallExplorationAlgorithm() {
        rballfio = (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(
				r, seed).initialize(pbf);
		PBSolution pbs = pbf.getRandomSolution();

		rball = rballfio.initialize(pbs);
		rball.setSeed(seed);
    }

    private void reportInstanceToStandardOutput() {
        pbf.writeTo(new OutputStreamWriter(System.out));
    }

    private void closeOutputFiles() {
        nodesFile.close();
		edgesFile.close();
		histogramFile.close();
		gpProgram.close();
    }

    private void reportStatistics() {
        System.out.println("Problem init time: "
				+ rballfio.getProblemInitTime());
		System.out
				.println("Solution init time: " + rball.getSolutionInitTime());
		System.out.println("Move time: " + (finalTime - initTime));
		System.out.println("Move+crossover time: " + (timeAfterCrossover - initTime));
		System.out.println("Stored scores:" + rballfio.getStoredScores());
		System.out.println("Var appearance (max):" + max_app);
		System.out.println("Var interaction (max):" + max_interactions);
    }

    private void applyPartitionCrossoverToAllPairsOfLocalOptima() {
        PartitionCrossover px = new PartitionCrossover(pbf);
		px.setSeed(seed);

		PBSolution[] los = localOptima.toArray(new PBSolution[0]);
		for (int i = 0; i < los.length; i++) {
			for (int j = i + 1; j < los.length; j++) {
				PBSolution res = px.recombine(los[i], los[j]);
				notifyCrossover(i, j, res);
			}
		}

		timeAfterCrossover = System.currentTimeMillis();
    }

    private void computeVariablesStatistics() {
        max_app = 0;
		max_interactions = 0;
		for (int i = 0; i < pbf.getN(); i++) {
			if (pbf.getAppearsIn()[i].length > max_app) {
				max_app = pbf.getAppearsIn()[i].length;
			}

			if (pbf.getInteractions()[i].length > max_interactions) {
				max_interactions = pbf.getInteractions()[i].length;
			}
		}
    }

    private void createAndOpenOutputFiles(String file_name) {
        try {
			nodesFile = new PrintWriter(new FileOutputStream(file_name
					+ ".nodes"));
			nodesFile.println("FITNESS");
			edgesFile = new PrintWriter(new FileOutputStream(file_name
					+ ".edges"));
			histogramFile = new PrintWriter(new FileOutputStream(file_name
					+ ".hist"));
			gpProgram = new PrintWriter(new FileOutputStream(file_name + ".gp"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("I cannot open the output files");
		}
    }

    private List<PBSolution> findLocalOptima() {
        initTime = System.currentTimeMillis();

		notifyLocalOptima(rball, pbf);
		for (int bit : new GrayCodeBitFlipIterable(pbf.getN())) {
			rball.moveOneBit(bit);
			notifyLocalOptima(rball, pbf);
		}

		finalTime = System.currentTimeMillis();
		
		return localOptima;
    }

	private void writeHistogram(int[] localOptimaHistogram) {

		List<SolutionFrequency> aux = new ArrayList<SolutionFrequency>();
		int index = 0;
		for (PBSolution sol : localOptima) {
			aux.add(new SolutionFrequency(sol, localOptimaHistogram[index++]));
		}

		Collections.sort(aux, new Comparator<SolutionFrequency>() {
			@Override
			public int compare(SolutionFrequency o1, SolutionFrequency o2) {
				return -Double.compare(o1.fitness, o2.fitness);
			}
		});

		for (SolutionFrequency sf : aux) {
			histogramFile.println(sf.fitness + " " + sf.frequency);
		}

	}

	private void writeGNUPlotProgram(String fileName) {
		gpProgram.println("set style histogram gap 5");
		gpProgram.println("set style data histograms");
		gpProgram.println("set style fill solid 1.0 border -1");
		gpProgram.println("set boxwidth 0.9 absolute");
		gpProgram.println("set xrange [0 : " + localOptima.size() + "]");
		gpProgram.println("set terminal postscript color solid");
		gpProgram.println("set output '" + fileName + ".ps'");
		gpProgram.println("plot '" + fileName + ".hist' using 2");
		gpProgram.println("exit");
	}

	private String wI(int i) {
		return "" + (i + 1);
	}

	private int edgeID(int i, int j, int kind) {
		return ((localOptima.size() * i + j)<< 1) + (kind-1);
	}

	private void notifyEdge(int i, int j, int kind) {
		int eid = edgeID(i, j, kind);
		if (!appearedEdges.contains(eid)) {
			appearedEdges.add(eid);
			edgesFile.println(wI(i) + " " + wI(j) + " "+kind);
		}
	}

	private void notifyCrossover(int i, int j, PBSolution res) {
		int index = localOptima.indexOf(res);

		if (index < 0 || (index != i && index != j)) {
			System.out.print("R:" + localOptima.get(i) + "(" + wI(i) + ") x "
					+ localOptima.get(j) + "(" + wI(j) + ") -> " + res
					+ (index < 0 ? "" : "(" + wI(index) + ")"));

			int kind=1;
			if (index < 0) {
				res = climbToLocalOptima(res);
				index = localOptima.indexOf(res);
				kind=2;
				if (index >= 0) {
					System.out.print(" -> " + res + "(" + wI(index) + ")");
					
				} else {
					System.out.print("Local Optima not found after climbing");
				}
			}

			System.out.println();

			if (index >= 0) {
				notifyEdge(i, index, kind);
				notifyEdge(j, index, kind);

				localOptimaHistogram[index]++;
			}

		}
	}

	private PBSolution climbToLocalOptima(PBSolution res) {
		RBallEfficientHillClimberSnapshot rball = (RBallEfficientHillClimberSnapshot) new RBallEfficientHillClimber(
				r, seed).initialize(pbf).initialize(res);

		double imp;
		do {
			imp = rball.move();

		} while (imp > 0);

		return rball.getSolution();
	}

}
