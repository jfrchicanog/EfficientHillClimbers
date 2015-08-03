package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.DoubleLinkedListBasedStore;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.util.linkedlist.Entry;

public class RBallEfficientHillClimberSnapshot implements
		HillClimberSnapshot<EmbeddedLandscape> {

	public static final String PROFILE = "profile";

	public static interface SubfunctionChangeListener {
		public void valueChanged(int subFunction, double oldValue, double newValue);
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

	public DoubleLinkedListBasedStore movesStore;
    /* Solution info */
	private int[] maxNomEmptyScore;
	/* Solution info */
	private int minImpRadius;
	/* Solution info */
	protected boolean collectFlips;

	// Used for soft restarts
	/* Solution info */
	protected Random rnd;

	// Auxiliary data structures
	private int[] flippedBits;
	private PBSolution sub;
	private PBSolution subSov;
	private Double[] subfnsEvals;
	protected double solutionQuality;

	// Listeners
	/* Solution info */
	private SubfunctionChangeListener scl;

	// Statistics of the operator
	/* Solution info */
	private int[] movesPerDistance;

	/* Solution info */
	private long solutionInitializationTime;
	private long solutionInitializationEvals;
	private long solutionMoveEvals;
	private long totalMoves;
	private long totalSolutionInitializations;
	private int[] flips;
	private List<ProfileData> profile;

	public RBallEfficientHillClimberSnapshot(
			RBallEfficientHillClimberForInstanceOf rballfio, PBSolution sol) {
		this.rballfio = rballfio;
		this.rball = rballfio.getHillClimber();
		radius = rball.radius;
		problem = rballfio.problem;
		collectFlips = rball.collect_flips;
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
		

		minImpRadius = radius + 1;
		movesPerDistance = new int[radius + 1];
		flippedBits = new int[radius];

		solutionInitializationEvals = 0;
		solutionMoveEvals = 0;
		totalMoves = 0;
		totalSolutionInitializations = 0;

	}

    private void initializeMaxScores(int radius) {
        maxNomEmptyScore = new int[radius + 1];
		for (int i = 1; i <= radius; i++) {
		    maxNomEmptyScore[i] = 0;
		}
    }


    /* Problem method /Sol method */
	private void initializeProblemDependentStructuresDarrell() {
		// This map is to implement a one-to-one function between SetOfVars and
		// integers (Minimal Perfect Hashing Function)
		if (collectFlips) {
			flips = new int[problem.getN()];
		}
		
		int buckets = 2 + ((rball.quality_limits == null) ? 0
                : rball.quality_limits.length);
        
		movesStore = new DoubleLinkedListBasedStore(radius, buckets, rballfio.minimalPerfectHash);
		
		initializeMaxScores(radius);
		
		sub = new PBSolution(rballfio.max_k);
		subSov = new PBSolution(rballfio.max_k);
		sol = null;
		subfnsEvals = new Double[problem.getM()];
	}

    /* Sol method */
	private void initializeSolutionDependentStructuresFromScratch() {
		long init = System.currentTimeMillis();
		// Compute the scores for all the values of the map
		for (int i = 1; i <= radius; i++) {
			maxNomEmptyScore[i] = 0;
		}

		double update;

		solutionQuality = 0;
		for (int sf = 0; sf < problem.getM(); sf++) {
			subfnsEvals[sf] = null;
		}

		for (RBallPBMove move : movesStore.iterableOverMoves()) {
            SetOfVars sov = move.flipVariables;

			update = 0;

			for (int sf : rballfio.subFunctionsAffected(sov)) {
				int k = problem.getMaskLength(sf);
				// For each subfunction do ...
				double vSub;
				if (subfnsEvals[sf] != null) {
					vSub = subfnsEvals[sf];
					fireChange(sf, Double.NaN, vSub);
				} else {

					for (int j = 0; j < k; j++) {
						int bit = problem.getMasks(sf, j);
						sub.setBit(j, sol.getBit(bit));
					}
					subfnsEvals[sf] = vSub = problem.evaluateSubfunction(sf,
							sub);
					solutionQuality += vSub;
				}

				// Build the subsolutions
				for (int j = 0; j < k; j++) {
					int bit = problem.getMasks(sf, j);
					subSov.setBit(j, sol.getBit(bit)
							^ (sov.contains(bit) ? 0x01 : 0x00));
				}

				double vSubSov = problem.evaluateSubfunction(sf, subSov);

				solutionInitializationEvals += 2;

				update += vSubSov - vSub;
			}
			double oldValue = move.improvement;
			move.improvement = update;

			int oldQualityIndex = rball.getQualityIndex(oldValue);
			int newQualityIndex = rball.getQualityIndex(update);

			if (oldQualityIndex != newQualityIndex) {
				int p = sov.size();
				movesStore.changeMoveBucketLIFO(p, oldQualityIndex, newQualityIndex, move);

				if (newQualityIndex > maxNomEmptyScore[p]) {
					// It is an improving move (necessarily, because
					// maxNomEmptyScore is 0 at least
					maxNomEmptyScore[p] = newQualityIndex;
				}
			}
		}

		// Update the maxNomEmptyScore and the minImpRadius (in case they are
		// overestimated)
		minImpRadius = radius + 1;
		for (int i = radius; i >= 1; i--) {
			while (maxNomEmptyScore[i] > 0
					&& movesStore.isBucketEmpty(i, maxNomEmptyScore[i])) {
				maxNomEmptyScore[i]--;
			}
			if (maxNomEmptyScore[i] > 0) {
				minImpRadius = i;
			}
		}

		totalSolutionInitializations++;

		solutionInitializationTime = System.currentTimeMillis() - init;

	}

    /* Solution method */
	private void initializeSolutionDependentStructuresFromSolution(
			PBSolution toThis) {
		long init = System.currentTimeMillis();
		// Move by doing partial updates
		int n = problem.getN();
		for (int v = 0; v < n; v++) {
			if (toThis.getBit(v) != this.sol.getBit(v)) {
				moveOneBit(v);
			}
		}
		solutionInitializationTime = System.currentTimeMillis() - init;
		totalSolutionInitializations++;
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
			return movesStore.getDeterministicMoveInBucket(minImpRadius, maxNomEmptyScore[minImpRadius]);
		}
	}

    /* Sol method */
	@Override
	public double move() {
		if (minImpRadius > radius) {
			return 0;
		}

		// else

		RBallPBMove move = movesStore.getDeterministicMoveInBucket(minImpRadius, maxNomEmptyScore[minImpRadius]);
		double imp = move.improvement;

		solutionQuality += imp;

		reportMovement(minImpRadius);
		moveSeveralBitsEff(move.flipVariables);

		return imp;
	}

	/* Sol method */
	private void reportMovement(int r) {
		movesPerDistance[r]++;
		if (profile != null) {
			ProfileData pd;
			if (!profile.isEmpty()) {
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

		// Identify which which subfunctions will be afffected

		for (int sf : rballfio.subFunctionsAffected(bits)) {
			int k = problem.getMaskLength(sf);
			// For each subfunction do ...
			// For each move score evaluate the subfunction and update the
			// corresponding value
			double vSub;

			int indI = 0;
			for (int j = 0; j < k; j++) {
				int bit = problem.getMasks(sf, j);
				sub.setBit(j, sol.getBit(bit));
				if (bits.contains(bit)) {
					flippedBits[indI++] = j;
				}
			}

			vSub = subfnsEvals[sf];

			for (int j = 0; j < indI; j++) {
				sub.flipBit(flippedBits[j]);
			}

			double vSubI = problem.evaluateSubfunction(sf, sub);

			solutionMoveEvals++;

			// Revert the solution to the current value
			for (int j = 0; j < indI; j++) {
				sub.flipBit(flippedBits[j]);
			}

			for (int entryIndex : rballfio.subfns[sf]) {
				RBallPBMove move = movesStore.getMoveByID(entryIndex);
                SetOfVars sov = move.flipVariables;

				// Build the subsolutions
				subSov.copyFrom(sub);

				for (int j = 0; j < k; j++) {
					int bit = problem.getMasks(sf, j);
					if (sov.contains(bit)) {
						subSov.flipBit(j);
					}
				}

				double vSubSov = problem.evaluateSubfunction(sf, subSov);

				solutionMoveEvals++;

				for (int j = 0; j < indI; j++) {
					subSov.flipBit(flippedBits[j]);
				}

				double vSubSovI = problem.evaluateSubfunction(sf, subSov);

				solutionMoveEvals += 1;

				double update = (vSubSovI - vSubI) - (vSubSov - vSub);

				double old = move.improvement;
				move.improvement += update;

				int oldQualityIndex = rball.getQualityIndex(old);
				int newQualityIndex = rball.getQualityIndex(old + update);

				if (oldQualityIndex != newQualityIndex) {
					int p = sov.size();
					
					if (rball.lifo) {
					    movesStore.changeMoveBucketLIFO(p, oldQualityIndex, newQualityIndex, move);
					    
					} else {
					    movesStore.changeMoveBucketFIFO(p, oldQualityIndex, newQualityIndex, move);
					    //movesStore.iterableOverBucket(p, oldQualityIndex).remove(entry);
						//movesStore.iterableOverBucket(p, newQualityIndex).append(entry);
					}

					if (newQualityIndex > maxNomEmptyScore[p]) {
						// It is an improving move (necessarily, because
						// maxNomEmptyScore is 0 at least
						maxNomEmptyScore[p] = newQualityIndex;
					}
				}
			}

			subfnsEvals[sf] = vSubI;
			fireChange(sf, vSub, vSubI);


		}

		// Update the maxNomEmptyScore and the minImpRadius (in case they are
		// overestimated)
		minImpRadius = radius + 1;
		for (int i = radius; i >= 1; i--) {
			while (maxNomEmptyScore[i] > 0
					&& movesStore.isBucketEmpty(i, maxNomEmptyScore[i])) {
				maxNomEmptyScore[i]--;
			}
			if (maxNomEmptyScore[i] > 0) {
				minImpRadius = i;
			}
		}

		// Finally, flip the bit in the solution and we are done
		for (int bit : bits) {
			sol.flipBit(bit);
			if (collectFlips) {
				flips[bit]++;
			}
		}

		totalMoves++;
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
		for (RBallPBMove move : movesStore.iterableOverMoves()) {
            SetOfVars sov = move.flipVariables;
			PBSolution solSov = new PBSolution(sol);

			for (int var : sov) {
				solSov.flipBit(var);
			}

			// Check the values of the scores
			double diff = problem.evaluate(solSov) - problem.evaluate(sol);
			assert move.improvement == diff : new RuntimeException("Expected "
					+ diff + " found " + move.improvement + " in " + sov);

		}
		// Check if they are in the correct list
		for (int p = 1; p <= radius; p++) {
			for (int q = 0; q < movesStore.getNumberOfBuckets(p); q++) {
				for (RBallPBMove move : movesStore.iterableOverBucket(p, q)) {
					assert move.flipVariables.size() == p;

					if (q == 0) {
						assert move.improvement <= 0;
					} else if (q == 1) {
						assert move.improvement > 0;
						if (rball.quality_limits != null) {
							assert move.improvement < rball.quality_limits[0];
						}
					} else {
						assert (move.improvement >= rball.quality_limits[q - 2]);
						if (q <= rball.quality_limits.length) {
							assert move.improvement < rball.quality_limits[q - 1];
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
	public void softRestart(int softRestart) {
		int n = problem.getN();
		boolean tmp = collectFlips;
		collectFlips = false;
		for (int i = 0; i < softRestart; i++) {
			int var = rnd.nextInt(n);
			RBallPBMove move = movesStore.getMoveByID(rballfio.oneFlipScores[var]);
            solutionQuality += move.improvement;
			moveOneBit(var);
		}
		collectFlips = tmp;
	}

	/* Sol method */
	public int[] getMovesPerDinstance() {
		return movesPerDistance;
	}

	/* Sol method */
	public void resetMovesPerDistance() {
		for (int i = 0; i < movesPerDistance.length; i++) {
			movesPerDistance[i] = 0;
		}
	}

	/* Sol method */
	public long getSolutionInitTime() {
		return solutionInitializationTime;
	}

	/* Sol method */
	public long getTotalMoves() {
		return totalMoves;
	}

	/* Sol method */
	public long getTotalSolutionInits() {
		return totalSolutionInitializations;
	}

	/* Sol method */
	public long getSubfnsEvalsInMoves() {
		return solutionMoveEvals;
	}

	/* Sol method */
	public long getSubfnsEvalsInSolInits() {
		return solutionInitializationEvals;
	}

	/* Sol method */
	public double getSolutionQuality() {
		return solutionQuality;
	}

	/* Sol method */
	protected void fireChange(int sf, double oldVal, double newVal) {
		if (scl != null) {
			scl.valueChanged(sf, oldVal, newVal);
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
