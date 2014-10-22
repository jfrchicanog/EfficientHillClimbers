package neo.landscape.theory.apps.pseudoboolean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfSetOfVars;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.util.DoubleLinkedList;
import neo.landscape.theory.apps.util.DoubleLinkedList.Entry;
import neo.landscape.theory.apps.util.IteratorFromArray;
import neo.landscape.theory.apps.util.RootedTreeGenerator;
import neo.landscape.theory.apps.util.RootedTreeGenerator.RootedTreeCallback;
import neo.landscape.theory.apps.util.Seeds;

public class RBallEfficientHillClimberSnapshot implements
		HillClimberSnapshot<EmbeddedLandscape> {

	public static final String PROFILE = "profile";

	public static interface SubfunctionChangeListener {
		public void valueChanged(int sf, double old_value, double new_value);
	}

	/* Solution info */
	public static class ProfileData {
		public ProfileData(int radius, int moves) {
			this.radius = radius;
			this.moves = moves;
		}

		public int radius;
		public int moves;
	}

	/* Operator dependent structures */
	RBallEfficientHillClimber rball;
	// Main configuration parameters and variables
	private final int radius; // This is just a convenient variable, since this
								// value is used a lot

	/* Problem dependent structures */
	RBallEfficientHillClimberForInstanceOf rballfio;
	/* Problem info */
	protected final EmbeddedLandscape problem; // A convenient copy

	/* Solution dependent structures */

	/* Solution info */
	protected PBSolution sol;

	// Main configuration parameters and variables

	/* Solution info */
	protected Entry<RBallPBMove>[] mos;
	/* Solution info */
	private DoubleLinkedList<RBallPBMove>[][] scores;
	/* Solution info */
	private int[] maxNomEmptyScore;
	/* Solution info */
	private int minImpRadius;
	/* Solution info */
	protected boolean collect_flips;

	// Used for soft restarts
	/* Solution info */
	protected Random rnd;

	// Auxiliary data structures
	private int[] flip_bits;
	private PBSolution sub;
	private PBSolution sub_sov;
	private Double[] subfns_evals;
	protected double sol_quality;

	// Listeners
	/* Solution info */
	private SubfunctionChangeListener scl;

	// Statistics of the operator
	/* Solution info */
	private int[] moves_per_distance;

	/* Solution info */
	private long solution_init_time;
	private long solution_init_evals;
	private long solution_move_evals;
	private long total_moves;
	private long total_sol_inits;
	private int[] flips;
	private List<ProfileData> profile;

	public RBallEfficientHillClimberSnapshot(
			RBallEfficientHillClimberForInstanceOf rballfio, PBSolution sol) {
		this.rballfio = rballfio;
		this.rball = rballfio.getHillClimber();
		radius = rball.radius;
		problem = rballfio.problem;
		collect_flips = rball.collect_flips;
		rnd = new Random(rball.rnd.nextLong());
		if (rball.configuration.containsKey(PROFILE)) {
			profile = new ArrayList<ProfileData>();
		}
		initializeOperatorDependentStructures();
		initializeProblemDependentStructuresDarrell();

		if (false && this.sol != null) {
			initializeSolutionDependentStructuresFromSolution((PBSolution) sol);
		} else {
			this.sol = (PBSolution) sol;
			initializeSolutionDependentStructuresFromScratch();
		}
	}

	/* Operator / problem /sol method */
	private void initializeOperatorDependentStructures() {
		/* Sol */
		scores = new DoubleLinkedList[radius + 1][2 + ((rball.quality_limits == null) ? 0
				: rball.quality_limits.length)];
		maxNomEmptyScore = new int[radius + 1];

		for (int i = 1; i <= radius; i++) {
			for (int j = 0; j < scores[i].length; j++) {
				scores[i][j] = new DoubleLinkedList<RBallPBMove>();
			}
			maxNomEmptyScore[i] = 0;
		}

		minImpRadius = radius + 1;
		moves_per_distance = new int[radius + 1];
		flip_bits = new int[radius];

		solution_init_evals = 0;
		solution_move_evals = 0;
		total_moves = 0;
		total_sol_inits = 0;

	}

	/* Problem method / Sol method */
	private void initializeProblemDependentStructures() {
		mos = new Entry[rballfio.minimalPerfectHash.size()];

		for (Map.Entry<SetOfVars, Integer> entry : rballfio.minimalPerfectHash
				.entrySet()) {
			SetOfVars sov = entry.getKey();
			RBallPBMove rmove = new RBallPBMove(0, sov);
			Entry<RBallPBMove> e = new Entry<RBallPBMove>(rmove);
			mos[entry.getValue()] = e;
			scores[sov.size()][0].add(e);
		}

		sub = new PBSolution(rballfio.max_k);
		sub_sov = new PBSolution(rballfio.max_k);
		sol = null;
		subfns_evals = new Double[problem.getM()];
	}

	/* Problem method /Sol method */
	private void initializeProblemDependentStructuresDarrell() {
		// This map is to implement a one-to-one function between SetOfVars and
		// integers (Minimal Perfect Hashing Function)
		if (collect_flips) {
			flips = new int[problem.getN()];
		}

		mos = new Entry[rballfio.minimalPerfectHash.size()];

		for (Map.Entry<SetOfVars, Integer> entry : rballfio.minimalPerfectHash
				.entrySet()) {
			SetOfVars sov = entry.getKey();
			RBallPBMove rmove = new RBallPBMove(0, sov);
			Entry<RBallPBMove> e = new Entry<RBallPBMove>(rmove);
			mos[entry.getValue()] = e;
			scores[sov.size()][0].add(e);
		}

		sub = new PBSolution(rballfio.max_k);
		sub_sov = new PBSolution(rballfio.max_k);
		sol = null;
		subfns_evals = new Double[problem.getM()];
	}

	/* Sol method */
	private void initializeSolutionDependentStructuresFromScratch() {
		long init = System.currentTimeMillis();
		// Compute the scores for all the values of the map
		// int [][] masks = problem.getMasks();
		for (int i = 1; i <= radius; i++) {
			maxNomEmptyScore[i] = 0;
		}

		double update;

		sol_quality = 0;
		for (int sf = 0; sf < problem.getM(); sf++) {
			subfns_evals[sf] = null;
		}

		for (Entry<RBallPBMove> e : mos) {
			SetOfVars sov = e.v.flipVariables;

			update = 0;

			for (int sf : rballfio.subFunctionsAffected(sov)) {
				int k = problem.getMaskLength(sf);// masks[sf].length;
				// For each subfunction do ...
				double v_sub;
				if (subfns_evals[sf] != null) {
					v_sub = subfns_evals[sf];
					fireChange(sf, Double.NaN, v_sub);
				} else {

					for (int j = 0; j < k; j++) {
						int bit = problem.getMasks(sf, j); // masks[sf][j];
						sub.setBit(j, sol.getBit(bit));
					}
					subfns_evals[sf] = v_sub = problem.evaluateSubfunction(sf,
							sub);
					sol_quality += v_sub;
				}

				// Build the subsolutions
				for (int j = 0; j < k; j++) {
					int bit = problem.getMasks(sf, j); // masks[sf][j];
					sub_sov.setBit(j, sol.getBit(bit)
							^ (sov.contains(bit) ? 0x01 : 0x00));
				}

				double v_sub_sov = problem.evaluateSubfunction(sf, sub_sov);

				solution_init_evals += 2;

				update += v_sub_sov - v_sub;
			}
			double old_value = e.v.improvement;
			e.v.improvement = update;

			int old_q_ind = rball.getQualityIndex(old_value);
			int new_q_ind = rball.getQualityIndex(update);

			if (old_q_ind != new_q_ind) {
				int p = sov.size();
				scores[p][old_q_ind].remove(e);
				scores[p][new_q_ind].add(e);

				if (new_q_ind > maxNomEmptyScore[p]) {
					// It is an improving move (necessarily, because
					// maxNomEmptyScore is 0 at least
					maxNomEmptyScore[p] = new_q_ind;
				}
			}
		}

		// Update the maxNomEmptyScore and the minImpRadius (in case they are
		// overestimated)
		minImpRadius = radius + 1;
		for (int i = radius; i >= 1; i--) {
			while (maxNomEmptyScore[i] > 0
					&& scores[i][maxNomEmptyScore[i]].isEmpty()) {
				maxNomEmptyScore[i]--;
			}
			if (maxNomEmptyScore[i] > 0) {
				minImpRadius = i;
			}
		}

		total_sol_inits++;

		solution_init_time = System.currentTimeMillis() - init;

		// System.out.println(solution_init_time);
	}

	/* Solution method */
	private void initializeSolutionDependentStructuresFromSolution(
			PBSolution to_this) {
		long init = System.currentTimeMillis();
		// Move by doing partial updates
		// SetOfVars aux = new SetOfVars();
		int n = problem.getN();
		for (int v = 0; v < n; v++) {
			if (to_this.getBit(v) != this.sol.getBit(v)) {
				moveOneBit(v);
			}
		}
		solution_init_time = System.currentTimeMillis() - init;
		total_sol_inits++;
	}

	/* Solution method */
	public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	/* Sol method */
	@Override
	public RBallPBMove getMovement() {
		if (minImpRadius > radius) {
			return new RBallPBMove(0, problem.getN());
		} else {
			return scores[minImpRadius][maxNomEmptyScore[minImpRadius]]
					.getFirst().v;
		}
	}

	/* Sol method */
	@Override
	public double move() {
		if (minImpRadius > radius) {
			return 0;
		}

		// else

		RBallPBMove move = scores[minImpRadius][maxNomEmptyScore[minImpRadius]]
				.getFirst().v;
		double imp = move.improvement;

		sol_quality += imp;

		reportMovement(minImpRadius);
		moveSeveralBitsEff(move.flipVariables);

		return imp;
	}

	/* Sol method */
	private void reportMovement(int r) {
		moves_per_distance[r]++;
		if (profile != null) {
			ProfileData pd;
			if (profile.size() > 0) {
				pd = profile.get(profile.size() - 1);
			} else {
				pd = new ProfileData(r, 0);
				profile.add(pd);
			}

			if (pd.radius == r) {
				pd.moves++;
			} else {
				pd = new ProfileData(r, 1);
				profile.add(pd);
			}
		}

	}

	/* Sol method */
	@Override
	public PBSolution getSolution() {
		return sol;
	}

	/* Sol method */
	protected void moveSeveralBitsEff(SetOfVars bits) {
		// int [][] masks = problem.getMasks();

		// Identify which which subfunctions will be afffected

		for (int sf : rballfio.subFunctionsAffected(bits)) {
			int k = problem.getMaskLength(sf); // masks[sf].length;
			// For each subfunction do ...
			// For each move score evaluate the subfunction and update the
			// corresponding value
			double v_sub;

			int ind_i = 0;
			for (int j = 0; j < k; j++) {
				int bit = problem.getMasks(sf, j); // masks[sf][j];
				sub.setBit(j, sol.getBit(bit));
				if (bits.contains(bit)) {
					flip_bits[ind_i++] = j;
				}
			}

			v_sub = subfns_evals[sf];

			// if (sf_values != null)
			// {
			// v_sub = [sf];
			// }
			// else
			// {
			// v_sub = problem.evaluateSubfunction(sf, sub);
			// solution_move_evals++;
			// }

			for (int j = 0; j < ind_i; j++) {
				sub.flipBit(flip_bits[j]);
			}

			double v_sub_i = problem.evaluateSubfunction(sf, sub);

			solution_move_evals++;

			// Revert the solution to the current value
			for (int j = 0; j < ind_i; j++) {
				sub.flipBit(flip_bits[j]);
			}

			for (int e_ind : rballfio.subfns[sf]) {
				Entry<RBallPBMove> e = mos[e_ind];
				SetOfVars sov = e.v.flipVariables;

				// Build the subsolutions
				sub_sov.copyFrom(sub);

				for (int j = 0; j < k; j++) {
					int bit = problem.getMasks(sf, j); // masks[sf][j];
					if (sov.contains(bit)) {
						sub_sov.flipBit(j);
					}

					// if (bits.contains(bit))
					// {
					// flip_bits[ind_i++]=j;
					// }
				}

				double v_sub_sov = problem.evaluateSubfunction(sf, sub_sov);

				solution_move_evals++;

				for (int j = 0; j < ind_i; j++) {
					// sub.flipBit(flip_bits[j]);
					sub_sov.flipBit(flip_bits[j]);
				}

				double v_sub_sov_i = problem.evaluateSubfunction(sf, sub_sov);
				// double v_sub_i = problem.evaluateSubfunction(sf, sub);

				solution_move_evals += 1;

				// for (int j=0; j < ind_i; j++)
				// {
				// sub.flipBit(flip_bits[j]);
				// }

				double update = (v_sub_sov_i - v_sub_i) - (v_sub_sov - v_sub);

				double old = e.v.improvement;
				e.v.improvement += update;

				int old_q_ind = rball.getQualityIndex(old);
				int new_q_ind = rball.getQualityIndex(old + update);

				if (old_q_ind != new_q_ind) {
					int p = sov.size();
					scores[p][old_q_ind].remove(e);
					if (rball.fifo) {
						scores[p][new_q_ind].add(e);
					} else {
						scores[p][new_q_ind].append(e);
					}

					if (new_q_ind > maxNomEmptyScore[p]) {
						// It is an improving move (necessarily, because
						// maxNomEmptyScore is 0 at least
						maxNomEmptyScore[p] = new_q_ind;
					}
				}
			}

			subfns_evals[sf] = v_sub_i;
			fireChange(sf, v_sub, v_sub_i);

			// if (sf_values != null)
			// {
			// sf_values[sf] = v_sub_i;
			// }

		}

		// Update the maxNomEmptyScore and the minImpRadius (in case they are
		// overestimated)
		minImpRadius = radius + 1;
		for (int i = radius; i >= 1; i--) {
			while (maxNomEmptyScore[i] > 0
					&& scores[i][maxNomEmptyScore[i]].isEmpty()) {
				maxNomEmptyScore[i]--;
			}
			if (maxNomEmptyScore[i] > 0) {
				minImpRadius = i;
			}
		}

		// Finally, flip the bit in the solution and we are done
		for (int bit : bits) {
			sol.flipBit(bit);
			if (collect_flips) {
				flips[bit]++;
			}
		}

		total_moves++;
	}

	/* Sol method */
	public void moveOneBit(int i) {
		moveSeveralBitsEff(SetOfVars.immutable(i));
	}

	/* Sol method */
	public int[] getFlipStat() {
		return flips;
	}

	/* Sol method */
	public void checkConsistency() {
		for (Entry<RBallPBMove> e : mos) {
			SetOfVars sov = e.v.flipVariables;
			PBSolution sol_sov = new PBSolution(sol);

			for (int var : sov) {
				sol_sov.flipBit(var);
			}

			// Check the values of the scores
			double diff = problem.evaluate(sol_sov) - problem.evaluate(sol);
			assert e.v.improvement == diff : new RuntimeException("Expected "
					+ diff + " found " + e.v.improvement + " in " + sov);

		}
		// Check if they are in the correct list
		for (int p = 1; p <= radius; p++) {
			for (int q = 0; q < scores[p].length; q++) {
				for (RBallPBMove move : scores[p][q]) {
					assert move.flipVariables.size() == p;

					if (q == 0) {
						assert (move.improvement <= 0);
					} else if (q == 1) {
						assert (move.improvement > 0);
						if (rball.quality_limits != null) {
							assert (move.improvement < rball.quality_limits[0]);
						}
					} else {
						assert (move.improvement >= rball.quality_limits[q - 2]);
						if (q <= rball.quality_limits.length) {
							assert (move.improvement < rball.quality_limits[q - 1]);
						}
					}
				}
			}
		}
	}

	/* SOl method */
	public List<ProfileData> getProfile() {
		return profile;
	}

	// TODO: I have to test the profile implementation and make it accessible
	// from the experiments classes

	/* Sol method */
	public void resetProfile() {
		profile = new ArrayList<ProfileData>();
	}

	/* Sol method */
	public void softRestart(int soft_restart) {
		int n = problem.getN();
		boolean tmp = collect_flips;
		collect_flips = false;
		for (int i = 0; i < soft_restart; i++) {
			int var = rnd.nextInt(n);
			sol_quality += mos[rballfio.oneFlipScores[var]].v.improvement;
			moveOneBit(var);
		}
		collect_flips = tmp;
	}

	/* Sol method */
	public int[] getMovesPerDinstance() {
		return moves_per_distance;
	}

	/* Sol method */
	public void resetMovesPerDistance() {
		for (int i = 0; i < moves_per_distance.length; i++) {
			moves_per_distance[i] = 0;
		}
	}

	/* Sol method */
	public long getSolutionInitTime() {
		return solution_init_time;
	}

	/* Sol method */
	public long getTotalMoves() {
		return total_moves;
	}

	/* Sol method */
	public long getTotalSolutionInits() {
		return total_sol_inits;
	}

	/* Sol method */
	public long getSubfnsEvalsInMoves() {
		return solution_move_evals;
	}

	/* Sol method */
	public long getSubfnsEvalsInSolInits() {
		return solution_init_evals;
	}

	/* Sol method */
	public double getSolutionQuality() {
		return sol_quality;
	}

	/* Sol method */
	protected void fireChange(int sf, double old_val, double new_val) {
		if (scl != null) {
			scl.valueChanged(sf, old_val, new_val);
		}
	}

	/* Sol method */
	public void setSubfunctionChangeListener(SubfunctionChangeListener scl) {
		this.scl = scl;
	}

	@Override
	public RBallEfficientHillClimberForInstanceOf getHillClimberForInstanceOf() {
		return rballfio;
	}

}
