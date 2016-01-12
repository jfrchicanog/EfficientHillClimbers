package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Random;

import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class RBallEfficientHillClimberSnapshot implements
		HillClimberSnapshot<EmbeddedLandscape>, MovesAndSubFunctionsInspector, MovesAndSubFunctionInspectorFactory {

	public static interface SubfunctionChangeListener {
		public void valueChanged(int subFunction, double oldValue, double newValue);
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

	protected MovesSelector<RBallPBMove> movesSelector;
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

	protected RBallHillClimberStatistics statistics;
    public RBallEfficientHillClimberSnapshot(
            RBallEfficientHillClimberForInstanceOf rballfio, PBSolution sol) {
	    this(rballfio, sol, null);
	}
	
    public RBallEfficientHillClimberSnapshot(
			RBallEfficientHillClimberForInstanceOf rballfio, PBSolution sol, MovesAndSubFunctionInspectorFactory inspectorFactory) {
		this.rballfio = rballfio;
		this.rball = rballfio.getHillClimber();
		radius = rball.radius;
		problem = rballfio.problem;
		
		long seed = rball.rnd.nextLong();
		rnd = new Random(seed);
		//System.out.println("Padre: "+seed);
		
		statistics =  new RBallHillClimberStatistics(radius, 
		        rball.configuration.containsKey(RBallHillClimberStatistics.PROFILE), 
		        rball.collectFlips, problem.getN());
		
		movesSelector = createMovesSelector();
		
		initializeAuxiliaryStructures();

		this.sol = (PBSolution) sol;
		initializeSolutionDependentStructuresFromScratch(inspectorFactory);

	}

    private void initializeAuxiliaryStructures() {
        flippedBits = new int[radius];
		sub = new PBSolution(this.rballfio.max_k);
        subSov = new PBSolution(this.rballfio.max_k);
        this.sol = null;
        subfnsEvals = new Double[problem.getM()];
    }

    private MovesSelector createMovesSelector() {
        if (rball.neutralMoves) {
            return new NeutralSelector(rnd.nextLong(), rball.maxNeutralPrbability, 
                    rnd, 
                    rball.radius, 
                    rballfio.minimalPerfectHash);
        } else {
            return new QualityBasedNonNeutralSelector(rball.randomMoves, 
                    rball.radius, 
                    rball.qualityLimits, 
                    rnd, 
                    rball.lifo, rballfio.minimalPerfectHash);
        }
    }

    /* Sol method */
	private void initializeSolutionDependentStructuresFromScratch(MovesAndSubFunctionInspectorFactory inspectorFactory) {
		statistics.startSolutionInitTime();

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
                update = computeMoveImprovementFromScratch(sov, inspectorFactory);
            } else {
                update = inspector.getMoveImprovementByID(moveID);
                for (int sf : rballfio.subFunctionsAffected(sov)) {
                    if (subfnsEvals[sf] == null) {
                        double vSub = subfnsEvals[sf] = inspector.getSubFunctionEvaluation(sf);
                        fireChange(sf, Double.NaN, vSub);
                        solutionQuality += vSub;
                    }
                }
            }
            
			double oldValue = move.improvement;
			move.improvement = update;

			movesSelector.changeScoreBucket(move, oldValue, update);			
		}

		statistics.stopSolutionInitTime();
		statistics.increaseTotalSolutionInitializations(1);

	}

    private double computeMoveImprovementFromScratch(SetOfVars sov, MovesAndSubFunctionInspectorFactory inspectorFactory) {
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

        	statistics.increaseSolInitEvals(2);

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

			statistics.increaseSolMoveEvals(1);

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

				for (int j = 0; j < indI; j++) {
					subSov.flipBit(flippedBits[j]);
				}

				double vSubSovI = problem.evaluateSubfunction(sf, subSov);

				statistics.increaseSolMoveEvals(2);

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
			statistics.increaseFlips(bit);
		}

		statistics.increaseTotalMoves(1);
	}

    /* Sol method */
	public void moveOneBit(int i) {
	    RBallPBMove move = movesSelector.getMoveByID(rballfio.oneFlipScores[i]);
        solutionQuality += move.improvement;
		moveSeveralBitsEff(SetOfVars.immutable(i));
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
		// Check the value of the solution
		assert solutionQuality == problem.evaluate(sol);
	}

    /* Sol method */
	public void softRestart(int softRestart) {
		int n = problem.getN();
		statistics.disableFlipsCollection();
		for (int i = 0; i < softRestart; i++) {
			int var = rnd.nextInt(n);
			moveOneBit(var);
		}
		statistics.enableFlipsCollection();
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

    @Override
    public MovesAndSubFunctionsInspector getInspectorForMove(SetOfVars setOfVars) {
        return this;
    }

    @Override
    public MovesAndSubFunctionsInspector getInspectorForSubFunction(int subFunction) {
        return this;
    }

}
