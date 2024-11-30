package neo.landscape.theory.apps.pseudoboolean.experiments.loma;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.parsers.NKLandscapesDimacsLikeReader;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverAllChildren;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import org.apache.commons.cli.Options;

public class LocalOptimaExperimentAllChildren implements Process {

	private class LatticeID {
		public PBSolution minimumSolution;
		public PBSolution mask;

		public LatticeID(PBSolution minimum, PBSolution mask) {
			this.minimumSolution = minimum;
			this.mask = mask;
		}

		public String toString() {
			return String.format("%s|%s", minimumSolution.toHex(), mask.toHex());
		}

		public int hashCode() {
			return toString().hashCode();
		}

		public boolean equals(Object o) {
			if (o instanceof LatticeID) {
				LatticeID other = (LatticeID) o;
				return minimumSolution.equals(other.minimumSolution) && mask.equals(other.mask);
			}
			return false;
		}
	}

	private class LatticeInfo {
		public long hitCount;
		public List<Integer> localOptimaIndices;
	}

	private final static Comparator<PBSolution> SOLUTION_COMPARATOR = Comparator.comparing(s->s.toString());

	protected List<PBSolution> localOptima;
	private PrintWriter nodesFile;
	private PrintWriter edgesFile;
	private PrintWriter histogramFile;
	private PrintWriter gpProgram;
	//private Set<Integer> appearedEdges;

	private int[] localOptimaHistogram;

	protected NKLandscapes pbf;
	protected int r;
	protected RBallEfficientHillClimberSnapshot rball;
	private long initTime;
	private long finalTime;
	private int max_app;
	private int max_interactions;
	private long timeAfterCrossover;
	protected RBallEfficientHillClimberForInstanceOf rballfio;
	protected long seed;
	private LocalOptimaNetworkGoldman goldman;
	private Map<LatticeID, LatticeInfo> latticeStatistics;

	public LocalOptimaExperimentAllChildren() {
		localOptima = new ArrayList<PBSolution>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the All the Local Optima of the search "
				+ "space using the Efficient RBall exploration algorithm";
	}

	@Override
	public String getID() {
		return "lo-all";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + " [<n> <k> <q> <circular> | -instance <instance file>] <r> [<seed>]";
	}

	private void notifyLocalOptima(RBallEfficientHillClimberSnapshot rball,
			NKLandscapes pbf) {
		if (checkLocalOptima(rball)) {
			addLocalOptima(rball, pbf);
		}
	}

	protected void addLocalOptima(RBallEfficientHillClimberSnapshot rball, NKLandscapes pbf) {
		PBSolution lo = new PBSolution(rball.getSolution());
		double val = pbf.evaluate(lo);
		localOptima.add(lo);
		nodesFile.println(val);
	}

	private boolean checkLocalOptima(RBallEfficientHillClimberSnapshot rball) {
		return rball.getMovement().getImprovement() <= 0.0;
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 2) {
			System.out.println(getInvocationInfo());
			return;
		}
		
		String file_name;
		if (args[0].equals("-instance")) {
			String instanceFile = args[1];
			file_name = "mk-"+instanceFile;
			NKLandscapesDimacsLikeReader instanceReader = new NKLandscapesDimacsLikeReader();
			try (FileReader reader = new FileReader(instanceFile)) {
				pbf = instanceReader.readInstance(reader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			r = Integer.parseInt(args[2]);
			seed = 0;
			if (args.length >= 4) {
				seed = Long.parseLong(args[3]);
			} else {
				seed = Seeds.getSeed();
			}
			
		} else {

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
			createInstance(n, k, q, circular);
			file_name = computeFileName(n, k, q, circular);
		}

		prepareEdgesList();

		goldman = new LocalOptimaNetworkGoldman();
		goldman.r = r;
		goldman.seed = seed;
		goldman.setPbf(pbf);
		goldman.prepareRBallExplorationAlgorithm();
		goldman.findLocalOptima();

		createAndOpenOutputFiles(file_name);

		// prepareRBallExplorationAlgorithm();

		localOptima = goldman.localOptima;

		Collections.sort(localOptima, Comparator.comparing(s->pbf.evaluate(s)));

		int i=0;
		for (PBSolution sol: localOptima) {
		    nodesFile.println(wI(i) + ": " + sol + ": " + pbf.evaluate(sol));
		    i++;
		}

		localOptimaHistogram = createLocalOptimaHistogram(localOptima);
		latticeStatistics = new HashMap<>();
		applyPartitionCrossoverToAllPairsOfLocalOptima();

		//writeHistogram(localOptimaHistogram);
		//writeGNUPlotProgram(file_name);

		computeVariablesStatistics();

		reportStatistics();
		reportInstanceToStandardOutput();
		reportLatticeToLatticeFile();
		closeOutputFiles();

	}

	private void reportLatticeToLatticeFile() {
		for (Map.Entry<LatticeID, LatticeInfo> entry : latticeStatistics.entrySet()) {
			LatticeID id = entry.getKey();
			LatticeInfo info = entry.getValue();
			edgesFile.print(id.toString() + "\t" + info.hitCount + "\t");
			info.localOptimaIndices.stream()
				.map(i -> Integer.toString(i))
				.reduce((a, b) -> a + "|" + b)
				.ifPresent(edgesFile::print);
			long lo = info.localOptimaIndices.stream().filter(i -> i > 0).count();
			edgesFile.print("\t" + info.localOptimaIndices.size() + "\t" + lo);
			edgesFile.println();

		}
	}

	private String computeFileName(String n, String k, String q, String circular) {
		return "nkq-" + n + "-" + k + "-" + q + "-" + circular
				+ "-" + r + "-" + seed;
	}

	private void prepareEdgesList() {
		//appearedEdges = new HashSet<Integer>();
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

	protected void prepareRBallExplorationAlgorithm() {
		rballfio = (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(
				r).initialize(pbf);
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
				+ goldman.rballfio.getProblemInitTime());
		System.out
		.println("Solution init time: " + goldman.rball.getSolutionInitTime());
		System.out.println("Move time: " + (finalTime - initTime));
		System.out.println("Move+crossover time: " + (timeAfterCrossover - initTime));
		System.out.println("Stored scores:" + goldman.rballfio.getStoredScores());
		System.out.println("Var appearance (max):" + max_app);
		System.out.println("Var interaction (max):" + max_interactions);
	}

	private void applyPartitionCrossoverToAllPairsOfLocalOptima() {
		PartitionCrossoverAllChildren px = new PartitionCrossoverAllChildren(pbf);
		px.setSeed(seed);

		PBSolution[] los = localOptima.toArray(new PBSolution[0]);
		for (int i = 0; i < los.length; i++) {
			for (int j = i + 1; j < los.length; j++) {
				PBSolution mask = los[i].xor(los[j]);
				List<PBSolution> res = px.getAllChildren(los[i], los[j]);
				List<Integer> localOptimaIndices = notifyCrossover(i, j, res);

				if (localOptimaIndices.size() > 2) {
					res.stream().min(SOLUTION_COMPARATOR)
						.map(min-> new LatticeID(min, mask))
						.ifPresent(id -> {
							latticeStatistics.compute(id, (k, info) -> {
								if (info == null) {
									info = new LatticeInfo();
									info.localOptimaIndices = localOptimaIndices;
								}
								info.hitCount++;
								return info;
							});
						});
				}
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
					+ ".lattices"));
			histogramFile = new PrintWriter(new FileOutputStream(file_name
					+ ".hist"));
			gpProgram = new PrintWriter(new FileOutputStream(file_name + ".gp"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("I cannot open the output files");
		}
	}

	protected List<PBSolution> findLocalOptima() {
		initTime = System.currentTimeMillis();

		notifyLocalOptima(rball, pbf);
		for (int bit : new GrayCodeBitFlipIterable(pbf.getN())) {
			rball.moveOneBit(bit);
			notifyLocalOptima(rball, pbf);
		}

		finalTime = System.currentTimeMillis();

		return localOptima;
	}


	private String wI(int i) {
		return "" + (i + 1);
	}

	private int edgeID(int i, int j, int kind) {
		return ((localOptima.size() * i + j)<< 1) + (kind-1);
	}

	private void notifyEdge(int i, int j, int kind) {
		int eid = edgeID(i, j, kind);
		//		if (!appearedEdges.contains(eid)) {
		//			appearedEdges.add(eid);
		//			edgesFile.println(wI(i) + " " + wI(j) + " "+kind);
		//		}
	}

	private List<Integer> notifyCrossover(int i, int j, List<PBSolution> allChildren) {
	    if (allChildren.size() <= 2) {
	        return Arrays.asList(i, j);
	    }

		List<Integer> results = new ArrayList<>();
	    for (PBSolution res: allChildren) {
			int index = localOptima.indexOf(res);
			if (index >= 0) {
				edgesFile.print(wI(index) + "\t");
				results.add((index+1));
			} else {
					res = climbToLocalOptima(res);
					index = localOptima.indexOf(res);

					if (index >= 0) {
						edgesFile.print("-"+wI(index) + "\t");
						results.add(-(index+1));
					} else {
						System.err.print("Local Optima not found after climbing");
					}
			}
            if (index >= 0) {
                localOptimaHistogram[index]++;
            }
	    }
		edgesFile.println();
		return results;
	}

	private PBSolution climbToLocalOptima(PBSolution res) {
		RBallEfficientHillClimberSnapshot rball = (RBallEfficientHillClimberSnapshot) new RBallEfficientHillClimber(
				r).initialize(pbf).initialize(res);

		double imp;
		do {
			imp = rball.move();

		} while (imp > 0);

		return rball.getSolution();
	}


}
