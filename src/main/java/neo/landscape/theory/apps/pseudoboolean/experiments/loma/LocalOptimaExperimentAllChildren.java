package neo.landscape.theory.apps.pseudoboolean.experiments.loma;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.experiments.EmbeddedLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.*;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverAllChildren;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.text.NumberFormatter;

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

		@Override
		public int hashCode() {
			return Objects.hash(minimumSolution, mask);
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

	private class LatticeStats {
		public long latticeSize;
		public long ways;
		public long localOptima;

		public LatticeStats(long latticeSize, long ways, long localOptima) {
			this.latticeSize = latticeSize;
			this.ways = ways;
			this.localOptima = localOptima;
		}

		public String toString() {
			return String.format("%d,%d,%d", latticeSize, ways, localOptima);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LatticeStats that = (LatticeStats) o;
			return latticeSize == that.latticeSize && ways == that.ways && localOptima == that.localOptima;
		}

		@Override
		public int hashCode() {
			return Objects.hash(latticeSize, ways, localOptima);
		}
	}

	private final static Comparator<PBSolution> SOLUTION_COMPARATOR = Comparator.comparing(s->s.toString());

	protected List<PBSolution> localOptima;
	private PrintWriter nodesFile;
	private PrintWriter latticeFile;
	private PrintWriter latticeStatsFile;
	private PrintWriter latticeVectorFile;
	//private Set<Integer> appearedEdges;

	private int[] localOptimaHistogram;

	protected EmbeddedLandscape pbf;
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
	private Map<LatticeID, LatticeInfo> latticeCollection;
	private Map<LatticeStats, Integer> latticeStatistics;

	private static final String MAXSAT_PROBLEM = "maxsat";
	private static final String NK_PROBLEM = "nk";
	private static final String WALSH_PROBLEM = "walsh";

	private static final String PROBLEM="problem";
	private static final String RADIUS_ARGUMENT = "r";
	private static final String PROBLEM_CHAR = "P";
	private static final String ALGORITHM_SEED_ARGUMENT = "aseed";
	private static final String NODE_FILE_ARGUMENT = "nodes";
	private static final String LATTICE_FILE_ARGUMENT = "lattices";
	private static final String LATTICE_STATS_FILE_ARGUMENT = "latStats";
	private static final String LATTICE_VECTORS_FILE_ARGUMENT = "latVectors";

	private Options options;
	private CommandLine commandLine;
	private EmbeddedLandscapeConfigurator problemConfigurator;
	private String problem;
	private NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);

	private final Map<String, EmbeddedLandscapeConfigurator> configurators = new HashMap<>();
	{
		configurators.put(MAXSAT_PROBLEM, new MAXSATConfigurator());
		configurators.put(NK_PROBLEM, new NKLandscapeConfigurator());
		configurators.put(WALSH_PROBLEM, new WalshBasedFunctionConfigurator());
		numberFormatter.setMaximumFractionDigits(4);
	}


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

	private Options getOptions() {
		if (options == null) {
			options = prepareOptions();
		}
		return options;
	}

	private Options prepareOptions() {
		Options options = new Options();
		options.addOption(RADIUS_ARGUMENT, true, "radius of the Hamming Ball hill climber");
		options.addOption(PROBLEM, true, "problem to be solved: "+configurators.keySet());
		options.addOption(ALGORITHM_SEED_ARGUMENT, true, "random seed for the algorithm (optional)");
		options.addOption(NODE_FILE_ARGUMENT, true, "file to store the local optima (optional)");
		options.addOption(LATTICE_FILE_ARGUMENT, true, "file to store the lattices (optional)");
		options.addOption(LATTICE_STATS_FILE_ARGUMENT, true, "file to store the lattice statistics (optional)");
		options.addOption(LATTICE_VECTORS_FILE_ARGUMENT, true, "file to store the lattice vectors (optional)");
		options.addOption(Option.builder(PROBLEM_CHAR)
			.numberOfArgs(2)
			.valueSeparator()
			.argName("property=value")
			.desc("properties for the problem")
			.build());

		return options;
	}

	protected void showOptions() {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(getID(), getOptions());

		try {
			Options problemOptions = new Options();
			getProblemConfigurator().prepareOptionsForProblem(problemOptions);
			helpFormatter.printHelp("Problem: "+problem, problemOptions);
		} catch (RuntimeException e) {
		}
	}

	private EmbeddedLandscapeConfigurator getProblemConfigurator() {
		if (problemConfigurator==null) {
			problemConfigurator = createEmbeddedLandscapeConfigurator();
		}
		return problemConfigurator;
	}

	protected EmbeddedLandscapeConfigurator createEmbeddedLandscapeConfigurator() {
		EmbeddedLandscapeConfigurator elc =  configurators.get(problem);
		if (elc == null) {
			throw new IllegalArgumentException("Problem "+problem+" is unknown");
		}
		return elc;
	}

	private void notifyLocalOptima(RBallEfficientHillClimberSnapshot rball,
			EmbeddedLandscape pbf) {
		if (checkLocalOptima(rball)) {
			addLocalOptima(rball, pbf);
		}
	}

	protected void addLocalOptima(RBallEfficientHillClimberSnapshot rball, EmbeddedLandscape pbf) {
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
		try {
			if (args.length == 0) {
				System.out.println(getInvocationInfo());
				showOptions();
				return;
			}

			commandLine = parseCommandLine(args);
			problem = commandLine.getOptionValue(PROBLEM);

			pbf = getProblemConfigurator().configureProblem(
				commandLine.getOptionProperties(PROBLEM_CHAR), System.out);

			if (commandLine.hasOption(RADIUS_ARGUMENT)) {
				r = Integer.parseInt(commandLine.getOptionValue(RADIUS_ARGUMENT));
			} else {
				throw new IllegalArgumentException("Radius (r) is required");
			}

			if (commandLine.hasOption(ALGORITHM_SEED_ARGUMENT)) {
				seed = Long.parseLong(commandLine.getOptionValue(ALGORITHM_SEED_ARGUMENT));
			} else {
				seed = Seeds.getSeed();
			}

			prepareOutputFiles();
			computeLocalOptima();
			Collections.sort(localOptima, Comparator.comparing(s -> pbf.evaluate(s)));

			outputLocalOptimaIfNeeded();

			localOptimaHistogram = createLocalOptimaHistogram(localOptima);
			latticeCollection = new HashMap<>();
			applyPartitionCrossoverToAllPairsOfLocalOptima();

			computeVariablesStatistics();
			reportStatistics();
			reportLatticeToLatticeFile();
			reportLatticeStatistics();
			reportLatticeVectors();
			closeOutputFiles();
		} catch (Exception e) {
			e.printStackTrace();
			showOptions();
		}

	}

	private void reportLatticeVectors() {
		if (latticeVectorFile != null) {
			latticeVectorFile.println("LatticeSize,LO,FVector,SumSamples,DVector");
			computeLatticeStatisticsIfNeeded();
			Map<Pair<Long, Long>,Pair<Long,Long>> vectors = latticeStatistics.entrySet().stream()
				.collect(Collectors.toMap(
				lst -> Pair.of(lst.getKey().latticeSize, lst.getKey().localOptima),
				lst -> Pair.of(lst.getKey().ways * lst.getValue(), (long)lst.getValue()),
				(a, b) -> Pair.of(a.getLeft() + b.getLeft(), a.getRight() + b.getRight()))
			);

			vectors.entrySet().stream()
				.sorted(Comparator.<Map.Entry<Pair<Long, Long>, Pair<Long, Long>>, Long>comparing(e -> e.getKey().getLeft())
								.thenComparing(e -> e.getKey().getRight()))
				.forEach(e -> {
					Pair<Long, Long> vector = e.getValue();
					latticeVectorFile.println(e.getKey().getLeft() + "," + e.getKey().getRight()
						+ "," + vector.getLeft() + "," + vector.getRight() + "," +
						numberFormatter.format((vector.getLeft() / (double)vector.getRight())));
				});
		}
	}

	private void reportLatticeStatistics() {
		if (latticeStatsFile != null) {
			latticeStatsFile.println("LatticeSize,Ways,LO,Samples");
			computeLatticeStatisticsIfNeeded();
			latticeStatistics
				.forEach((k, v) -> latticeStatsFile.println(k + "," + v));
		}
	}

	private void computeLatticeStatisticsIfNeeded() {
		if (latticeStatistics == null) {
			latticeStatistics = latticeCollection.values().stream()
				.map(li ->
					new LatticeStats(li.localOptimaIndices.size(), li.hitCount,
						(int) li.localOptimaIndices.stream().filter(i -> i > 0).count()))
				.collect(Collectors.toMap(Function.identity(), v -> 1, Integer::sum));
		}
	}

	private void outputLocalOptimaIfNeeded() {
		if (nodesFile != null) {
			int i = 0;
			nodesFile.println("ID,Solution,Evaluation"); // Header
			for (PBSolution sol : localOptima) {
				nodesFile.println(wI(i) + "," + sol + "," + pbf.evaluate(sol));
				i++;
			}
		}
	}

	private void computeLocalOptima() {
		goldman = new LocalOptimaNetworkGoldman();
		goldman.r = r;
		goldman.seed = seed;
		goldman.setPbf(pbf);
		goldman.prepareRBallExplorationAlgorithm();
		goldman.findLocalOptima();
		localOptima = goldman.localOptima;
	}

	private void prepareOutputFiles() {
		if (commandLine.hasOption(NODE_FILE_ARGUMENT)) {
			try {
				nodesFile = new PrintWriter(new FileOutputStream(commandLine.getOptionValue(NODE_FILE_ARGUMENT)));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("I cannot open the output file for the local optima");
			}
		}

		if (commandLine.hasOption(LATTICE_FILE_ARGUMENT)) {
			try {
				latticeFile = new PrintWriter(new FileOutputStream(commandLine.getOptionValue(LATTICE_FILE_ARGUMENT)));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("I cannot open the output file for the lattices");
			}
		}

		if (commandLine.hasOption(LATTICE_STATS_FILE_ARGUMENT)) {
			try {
				latticeStatsFile = new PrintWriter(new FileOutputStream(commandLine.getOptionValue(LATTICE_STATS_FILE_ARGUMENT)));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("I cannot open the output file for the lattice statistics");
			}
		}

		if (commandLine.hasOption(LATTICE_VECTORS_FILE_ARGUMENT)) {
			try {
				latticeVectorFile = new PrintWriter(new FileOutputStream(commandLine.getOptionValue(LATTICE_VECTORS_FILE_ARGUMENT)));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("I cannot open the output file for the lattice vectors");
			}
		}
	}

	private CommandLine parseCommandLine(String[] args) {
		try {
			CommandLineParser parser = new DefaultParser();
			return parser.parse(getOptions(), args);
		} catch (ParseException e) {
			throw new RuntimeException (e);
		}
	}

	private void reportLatticeToLatticeFile() {
		if (latticeFile != null) {
			latticeFile.println("ID,Ways,LocalOptimaIndices,LocalOptima,LatticeSize");
			for (Map.Entry<LatticeID, LatticeInfo> entry : latticeCollection.entrySet()) {
				LatticeID id = entry.getKey();
				LatticeInfo info = entry.getValue();
				latticeFile.print(id.toString() + "," + info.hitCount + ",");
				info.localOptimaIndices.stream()
					.map(i -> Integer.toString(i))
					.reduce((a, b) -> a + "|" + b)
					.ifPresent(latticeFile::print);
				long lo = info.localOptimaIndices.stream().filter(i -> i > 0).count();
				latticeFile.print("," + info.localOptimaIndices.size() + "," + lo);
				latticeFile.println();

			}
		}
	}

	private int[] createLocalOptimaHistogram(List<PBSolution> localOptima) {
		localOptimaHistogram = new int[localOptima.size()];
		return localOptimaHistogram;
	}

	private void closeOutputFiles() {
		Stream.of(nodesFile, latticeFile, latticeStatsFile, latticeVectorFile)
			.filter(Objects::nonNull)
			.forEach(PrintWriter::close);
	}

	private void reportStatistics() {
		System.out.println("Problem init time: "
				+ goldman.rballfio.getProblemInitTime());
		System.out
		.println("Solution init time: " + goldman.rball.getStatistics().getSolutionInitTime());
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
			for (int j = i+1; j < los.length; j++) {
				int finalI=i;
				int finalJ=j;
				PBSolution mask = los[i].xor(los[j]);
				List<PBSolution> res = px.getAllChildren(los[i], los[j]);
				//List<Integer> localOptimaIndices = notifyCrossover(i, j, res);
				if (res.size() > 2) {
					res.stream().min(SOLUTION_COMPARATOR)
						.map(min->
							new LatticeID(new PBSolution(min), mask))
						.ifPresent(id -> {
							latticeCollection.compute(id, (k, info) -> {
								if (info == null) {
									info = new LatticeInfo();
									info.localOptimaIndices = notifyCrossover(finalI, finalJ, res);
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
				results.add((index + 1));
			} else {
				res = climbToLocalOptima(res);
				index = localOptima.indexOf(res);
				if (index >= 0) {
					results.add(-(index + 1));
				} else {
					System.err.print("Local Optima not found after climbing");
				}
			}
			if (index >= 0) {
				localOptimaHistogram[index]++;
			}
		}
		return results;
	}

	private PBSolution climbToLocalOptima(PBSolution res) {
		Properties rballConfig = new Properties();
		rballConfig.setProperty(RBallEfficientHillClimber.R_STRING, r+"");
		rballConfig.setProperty(RBallEfficientHillClimber.SEED, seed+"");
		RBallEfficientHillClimberSnapshot rball = (RBallEfficientHillClimberSnapshot) new RBallEfficientHillClimber(rballConfig)
			.initialize(pbf)
			.initialize(res);

		try {
			do {
				rball.move();
			} while (true);
		} catch (NoImprovingMoveException e) {

		}

		return rball.getSolution();
	}


}
