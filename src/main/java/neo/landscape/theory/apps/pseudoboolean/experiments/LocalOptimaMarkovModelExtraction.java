package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Triple;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.px.PartitionCrossoverAllChildren;
import neo.landscape.theory.apps.util.GrayCodeBitFlipIterable;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class LocalOptimaMarkovModelExtraction implements Process {

	protected List<PBSolution> localOptima;
	private PrintWriter markovModel;

	protected NKLandscapes pbf;
	protected int r;
    protected RBallEfficientHillClimberSnapshot rball;
    protected RBallEfficientHillClimberForInstanceOf rballfio;
    protected long seed;
	private LocalOptimaNetworkGoldman goldman;
	private Map<Triple<PBSolution, Integer, PBSolution>, Integer> markovSample;

	public LocalOptimaMarkovModelExtraction() {
		localOptima = new ArrayList<PBSolution>();
		markovSample = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the information required to compute a Markov model "
				+ "over the local optima";
	}

	@Override
	public String getID() {
		return "lo-markov-extraction";
	}

	@Override
	public String getInvocationInfo() {
		return "Arguments: " + getID() + "<n> <k> <q> <circular> <r> [<seed>]";
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

		createInstance(n, k, q, circular);
        
        
        goldman = new LocalOptimaNetworkGoldman();
        goldman.r = r;
        goldman.seed = seed;
        goldman.setPbf(pbf);
        goldman.prepareRBallExplorationAlgorithm();
        goldman.findLocalOptima();
        
        markovModel = new PrintWriter(System.out, true);

		// prepareRBallExplorationAlgorithm();

		localOptima = goldman.localOptima;
		
		Collections.sort(localOptima, Comparator.comparing(s->pbf.evaluate(s)));
		
		PBSolution solution = new PBSolution(pbf.getN());
		processSolution(solution);
		for (int bit : new GrayCodeBitFlipIterable(pbf.getN())) {
			solution.flipBit(bit);
			processSolution(solution);
		}
		
		PartitionCrossoverAllChildren px = new PartitionCrossoverAllChildren(pbf);
		px.setSeed(seed);
		
		// Printing header
		markovModel.print("LOX\tFITNESS_X\tLOY\tLOZ\t");
		for (int d=0; d < pbf.getN(); d++) {
			markovModel.print("D"+d+"\t");
		}
		markovModel.println("D"+pbf.getN());
		// Printing data
		for (int indX = 0; indX < localOptima.size(); indX++) {
			PBSolution x = localOptima.get(indX);
			double fitness = pbf.evaluate(x);
			for (int indY=0; indY < localOptima.size(); indY++) {
				PBSolution y = localOptima.get(indY);
				PBSolution z = px.recombine(x, y);
				z = climbToLocalOptima(z);
				int indZ = localOptima.indexOf(z);
				markovModel.print(indX+"\t"+fitness+"\t"+indY+"\t"+indZ+"\t");
				for (int d=0; d < pbf.getN(); d++) {
					markovModel.print(markovSample.getOrDefault(Triple.of(x, d, y), 0)+"\t");
				}
				markovModel.println(markovSample.getOrDefault(Triple.of(x, pbf.getN(), y), 0));
			}
		}
		markovModel.close();
		
	}
	
	private void processSolution(PBSolution z) {
		PBSolution aux = new PBSolution(z);
		PBSolution y = climbToLocalOptima(aux);
		for (PBSolution x: localOptima) {
			int d = x.hammingDistance(z);
			Triple<PBSolution, Integer, PBSolution> t = Triple.of(x, d, y);
			markovSample.compute(t, (k,v)->(v==null)?1:(v+1));
		}
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
