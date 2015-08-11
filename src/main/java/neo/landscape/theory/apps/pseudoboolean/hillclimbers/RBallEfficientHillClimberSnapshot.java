package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Random;

import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class RBallEfficientHillClimberSnapshot implements
		HillClimberSnapshot<EmbeddedLandscape>, MovesAndSubFunctionsInspector {

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

	public MovesSelector movesSelector;
    /* Solution info */
	protected boolean collectFlips;

	// Used for soft restarts
	/* Solution info */
	protected Random rnd;
	
	// For getMovement
	protected RBallPBMove nextMove;

	// Auxiliary data structures
	private int[] flippedBits;
	private PBSolution sub;
	private PBSolution subSov;
	private Double[] subfnsEvals;
	protected double solutionQuality;

	// Listeners
	/* Solution info */
	private SubfunctionChangeListener scl;

	/* Solution info */
	private long solutionInitializationTime;
	private long solutionInitializationEvals;
	private long solutionMoveEvals;
	private long totalMoves;
	private long totalSolutionInitializations;
	private int[] flips;
	private RBallHillClimberStatistics statistics;

    public RBallEfficientHillClimberSnapshot(
			RBallEfficientHillClimberForInstanceOf rballfio, PBSolution sol) {
		this.rballfio = rballfio;
		this.rball = rballfio.getHillClimber();
		radius = rball.radius;
		problem = rballfio.problem;
		collectFlips = rball.collect_flips;
		long seed = rball.rnd.nextLong();
		rnd = new Random(seed);
		//System.out.println("Padre: "+seed);
		
		statistics =  new RBallHillClimberStatistics(radius, rball.configuration.containsKey(RBallEfficientHillClimberSnapshot.PROFILE));
		
		movesSelector = new DeterministicQualityBasedNonNeutralSelector(this);
		initializeOperatorDependentStructures();
		initializeProblemDependentStructuresDarrell();

		if (false && this.sol != null) {
			initializeSolutionDependentStructuresFromSolution((PBSolution) sol);
		} else {
			this.sol = (PBSolution) sol;
			initializeSolutionDependentStructuresFromScratch(null);
		}
	}

    /* Operator / problem /sol method */
	private void initializeOperatorDependentStructures() {
		/* Sol */
		
		
		flippedBits = new int[radius];

		solutionInitializationEvals = 0;
		solutionMoveEvals = 0;
		totalMoves = 0;
		totalSolutionInitializations = 0;

	}

    /* Problem method /Sol method */
	private void initializeProblemDependentStructuresDarrell() {
		// This map is to implement a one-to-one function between SetOfVars and
		// integers (Minimal Perfect Hashing Function)
		if (collectFlips) {
			flips = new int[problem.getN()];
		}
		
		
		sub = new PBSolution(rballfio.max_k);
		subSov = new PBSolution(rballfio.max_k);
		sol = null;
		subfnsEvals = new Double[problem.getM()];
	}

    /* Sol method */
	private void initializeSolutionDependentStructuresFromScratch(MovesAndSubFunctionInspectorFactory inspectorFactory) {
		long init = System.currentTimeMillis();
		// Compute the scores for all the values of the map

		solutionQuality = 0;
		for (int sf = 0; sf < problem.getM(); sf++) {
			subfnsEvals[sf] = null;
		}

		for (int moveID=0; moveID < movesSelector.getNumberOfMoves(); moveID++) {
		    RBallPBMove move = movesSelector.getMoveByID(moveID);
            SetOfVars sov = move.flipVariables;
            
            double update=0;
            
            MovesAndSubFunctionsInspector inspector;
            if (inspectorFactory == null || 
                    (inspector = inspectorFactory.getInspectorForMove(sov)) == null) {
                update = computMoveImprovementFromScratch(sov, inspectorFactory);
            } else {
                update = inspector.getMoveImprovementByID(moveID);
                for (int sf : rballfio.subFunctionsAffected(sov)) {
                    if (subfnsEvals[sf] == null) {
                        subfnsEvals[sf] = inspector.getSubFunctionEvaluation(sf);
                    }
                }
            }
            
			double oldValue = move.improvement;
			move.improvement = update;

			movesSelector.changeScoreBucket(move, oldValue, update);			
		}

		totalSolutionInitializations++;

		solutionInitializationTime = System.currentTimeMillis() - init;

	}

    private double computMoveImprovementFromScratch(SetOfVars sov, MovesAndSubFunctionInspectorFactory inspectorFactory) {
        double update = 0;
        for (int sf : rballfio.subFunctionsAffected(sov)) {
        	int k = problem.getMaskLength(sf);
        	// For each subfunction do ...
        	double vSub;
        	if (subfnsEvals[sf] != null) {
        		vSub = subfnsEvals[sf];					
        	} else {
        	    MovesAndSubFunctionsInspector inspector;
        	    if (inspectorFactory == null ||
        	            (inspector=inspectorFactory.getInspectorForSubFunction(sf)) == null) {
        	        vSub = computeSubFunctionEvaluation(sf);
        	    } else {
        	        vSub = inspector.getSubFunctionEvaluation(sf);
        	    }
        	    
        	    subfnsEvals[sf] = vSub;
                fireChange(sf, Double.NaN, vSub);
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
        return update;
    }

    private double computeSubFunctionEvaluation(int sf) {
        int k = problem.getMaskLength(sf);
        for (int j = 0; j < k; j++) {
        	int bit = problem.getMasks(sf, j);
        	sub.setBit(j, sol.getBit(bit));
        }
        return problem.evaluateSubfunction(sf, sub);
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
	    //System.out.println(seed);
		rnd = new Random(seed);
	}

	/* Sol method */
	@Override
	public RBallPBMove getMovement() {
	    try {
	        if (nextMove == null) {
	            nextMove=movesSelector.getMovement();
	        }
	    } catch (NoImprovingMoveException e) {
	    }
	    return nextMove;
		
	}

    /* Sol method */
	@Override
	public double move() {
	    
	    RBallPBMove move;
	    if (nextMove == null) {
            move = movesSelector.getMovement();;
        } else {
            move = nextMove;
            nextMove=null;
        }

		double imp = move.improvement;

		solutionQuality += imp;

		statistics.reportMovement(move);
		moveSeveralBitsEff(move.flipVariables);

		return imp;
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
				RBallPBMove move = movesSelector.getMoveByID(entryIndex);
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

				movesSelector.changeScoreBucket(move, old, move.improvement);
			}

			subfnsEvals[sf] = vSubI;
			fireChange(sf, vSub, vSubI);


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
		for (RBallPBMove move : movesSelector.allMoves()) {
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
		movesSelector.checkCorrectPositionOfMovesInSelector();
	}

    /* Sol method */
	public void softRestart(int softRestart) {
		int n = problem.getN();
		boolean tmp = collectFlips;
		collectFlips = false;
		for (int i = 0; i < softRestart; i++) {
			int var = rnd.nextInt(n);
			RBallPBMove move = movesSelector.getMoveByID(rballfio.oneFlipScores[var]);
            solutionQuality += move.improvement;
			moveOneBit(var);
		}
		collectFlips = tmp;
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

    public RBallHillClimberStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public double getSubFunctionEvaluation(int subFunction) {
        return subfnsEvals[subFunction];
    }
    
    @Override
    public double getMoveImprovementByID(int id) {
        return movesSelector.getMoveByID(id).improvement;
    }

}
