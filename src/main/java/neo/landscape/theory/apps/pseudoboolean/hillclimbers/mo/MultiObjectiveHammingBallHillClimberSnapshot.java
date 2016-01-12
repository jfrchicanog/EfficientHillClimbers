package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

import java.util.Arrays;
import java.util.Random;

import neo.landscape.theory.apps.efficienthc.mo.MultiobjectiveHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionsInspector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKSubfunctionTranslator;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class MultiObjectiveHammingBallHillClimberSnapshot implements
		MultiobjectiveHillClimberSnapshot<VectorMKLandscape> {

	public static interface SubfunctionChangeListener {
		public void valueChanged(int subFunction, double oldValue, double newValue);
	}

	
	protected MultiObjectiveHammingBallHillClimber rball;
	private final int radius;

	MultiObjectiveHammingBallHillClimberForInstanceOf rballfio;
	
	protected final VectorMKLandscape problem;
	protected PBSolution sol;

	protected VectorMovesSelector<VectorPBMove> movesSelector;
	protected Random rnd;
	protected VectorPBMove nextMove;

	private int[] flippedBits;
	private PBSolution sub;
	private PBSolution subSov;
	private Double[] subfnsEvals;
	protected double [] solutionQuality;
	private double [] tempUpdate;

	private SubfunctionChangeListener scl;
	protected MultiObjectiveHammingBallHillClimberStatistics statistics;
    private VectorMKSubfunctionTranslator subfunctionsTranslator;
	
    
    public MultiObjectiveHammingBallHillClimberSnapshot(
            MultiObjectiveHammingBallHillClimberForInstanceOf rballfio,
            double [] weights,
            PBSolution sol) {
		this.rballfio = rballfio;
		this.rball = rballfio.getHillClimber();
		radius = rball.radius;
		problem = rballfio.problem;
		subfunctionsTranslator = problem.getSubfunctionsTranslator();
		
		long seed = rball.rnd.nextLong();
		rnd = new Random(seed);
		
		statistics =  new MultiObjectiveHammingBallHillClimberStatistics(radius, 
		        rball.configuration.containsKey(MultiObjectiveHammingBallHillClimberStatistics.PROFILE), 
		        rball.collectFlips, problem.getN());
		
		movesSelector = createMovesSelector(weights);
		
		initializeAuxiliaryStructures();

		this.sol = (PBSolution) sol;
		initializeSolutionDependentStructuresFromScratch();
        

	}

    private void initializeAuxiliaryStructures() {
        flippedBits = new int[radius];
		sub = new PBSolution(this.rballfio.max_k);
        subSov = new PBSolution(this.rballfio.max_k);
        this.sol = null;
        subfnsEvals = new Double[problem.getM()];
        tempUpdate = new double [problem.getDimension()];
    }

    private VectorMovesSelector<VectorPBMove> createMovesSelector(double [] weights) {
        return new MultiObjectiveSelector(rball.randomMoves, radius, rnd, rballfio.minimalPerfectHash, weights);
    }

	private void initializeSolutionDependentStructuresFromScratch() {
		statistics.startSolutionInitTime();

		solutionQuality = new double [problem.getDimension()];
		for (int sf = 0; sf < problem.getM(); sf++) {
			subfnsEvals[sf] = null;
		}

		for (int moveID=0; moveID < movesSelector.getNumberOfMoves(); moveID++) {
		    VectorPBMove move = movesSelector.getMoveByID(moveID);
            SetOfVars sov = move.flipVariables;
            
            double [] improvement = computeMoveImprovementFromScratch(sov);
			System.arraycopy(improvement, 0, move.deltas, 0, improvement.length);
			movesSelector.unclassifyMove(move);			
		}
		movesSelector.assignBucketsToUnclassifiedMoves();

		statistics.stopSolutionInitTime();
		statistics.increaseTotalSolutionInitializations(1);
	}

    private double [] computeMoveImprovementFromScratch(SetOfVars sov) {
        for (int i = 0; i < tempUpdate.length; i++) {
            tempUpdate[i]=0;
        }
        
        for (int sf : rballfio.subFunctionsAffected(sov)) {
            int dim = subfunctionsTranslator.dimensionOfSunbfunctionID(sf);
        	int k = problem.getMaskLength(sf);
        	// For each subfunction do ...
        	double vSub;
        	if (subfnsEvals[sf] != null) {
        		vSub = subfnsEvals[sf];					
        	} else {
        	    vSub = computeSubFunctionEvaluation(sf);
        	    subfnsEvals[sf] = vSub;
                fireChange(sf, Double.NaN, vSub);
                solutionQuality[dim] += vSub;
        	}

        	// Build the subsolutions
        	for (int j = 0; j < k; j++) {
        		int bit = problem.getMasks(sf, j);
        		subSov.setBit(j, sol.getBit(bit)
        				^ (sov.contains(bit) ? 0x01 : 0x00));
        	}
        	double vSubSov = problem.evaluateSubfunction(sf, subSov);

        	statistics.increaseSolInitEvals(2);

        	tempUpdate[dim] += vSubSov - vSub;
        }
        return tempUpdate;
    }

    private double computeSubFunctionEvaluation(int sf) {
        int k = problem.getMaskLength(sf);
        for (int j = 0; j < k; j++) {
        	int bit = problem.getMasks(sf, j);
        	sub.setBit(j, sol.getBit(bit));
        }
        return problem.evaluateSubfunction(sf, sub);
    }

	public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	@Override
	public VectorPBMove getMovement() {
	    if (nextMove == null) {
            nextMove=movesSelector.getMovement();
        }
	    return nextMove;
	}

	@Override
	public double [] move() {
	    VectorPBMove move;
	    if (nextMove == null) {
            move = movesSelector.getMovement();
        } else {
            move = nextMove;
            nextMove=null;
        }

		double [] delta = move.getImprovement();

		addVectors(solutionQuality, delta);

		statistics.reportMovement(move);
		moveSeveralBitsEff(move.flipVariables);

		return delta;
	}
	
	public VectorMovesSelector<VectorPBMove> getMovesSelector() {
	    return movesSelector;
	}

	private void addVectors(double[] vector1, double[] vector2) {
	    for (int i = 0; i < vector1.length; i++) {
            vector1[i] += vector2[i];
        }
    }

	@Override
	public PBSolution getSolution() {
		return sol;
	}

	protected void moveSeveralBitsEff(SetOfVars bits) {
		for (int sf : rballfio.subFunctionsAffected(bits)) {
			int k = problem.getMaskLength(sf);
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
				VectorPBMove move = movesSelector.getMoveByID(entryIndex);
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

				move.deltas[subfunctionsTranslator.dimensionOfSunbfunctionID(sf)] += update;

				movesSelector.unclassifyMove(move);
			}

			subfnsEvals[sf] = vSubI;
			fireChange(sf, vSub, vSubI);

		}

		movesSelector.assignBucketsToUnclassifiedMoves();
		
		// Finally, flip the bit in the solution and we are done
		for (int bit : bits) {
			sol.flipBit(bit);
			statistics.increaseFlips(bit);
		}

		statistics.increaseTotalMoves(1);
	}

	public void moveOneBit(int i) {
	    VectorPBMove move = movesSelector.getMoveByID(rballfio.oneFlipScores[i]);
	    addVectors(solutionQuality, move.getImprovement());
		moveSeveralBitsEff(SetOfVars.immutable(i));
	}

	public void checkConsistency() {
	    double [] oldEval = problem.evaluate(sol);
		for (VectorPBMove move : movesSelector.allMoves()) {
            SetOfVars sov = move.flipVariables;
			PBSolution solSov = new PBSolution(sol);

			for (int var : sov) {
				solSov.flipBit(var);
			}

			// Check the values of the scores
			double [] newEval = problem.evaluate(solSov);
			for (int i = 0; i < newEval.length; i++) {
			    double diff = newEval[i]-oldEval[i];
                assert move.getImprovement()[i] == (newEval[i]-oldEval[i]) : 
                    new RuntimeException("Expected "
                        + diff + " found " + move.getImprovement()[i] + " in dimension " + i 
                        + " of " + sov);
            }
		}
		
		movesSelector.checkCorrectPositionOfMovesInSelector();
		assert Arrays.equals(solutionQuality, oldEval);
	}

	public void softRestart(int softRestart) {
		int n = problem.getN();
		statistics.disableFlipsCollection();
		for (int i = 0; i < softRestart; i++) {
			int var = rnd.nextInt(n);
			moveOneBit(var);
		}
		statistics.enableFlipsCollection();
	}

	public double [] getSolutionQuality() {
		return solutionQuality;
	}

	protected void fireChange(int sf, double oldVal, double newVal) {
		if (scl != null) {
			scl.valueChanged(sf, oldVal, newVal);
		}
	}

	public void setSubfunctionChangeListener(SubfunctionChangeListener scl) {
		this.scl = scl;
	}

	@Override
	public MultiObjectiveHammingBallHillClimberForInstanceOf getHillClimberForInstanceOf() {
		return rballfio;
	}

    public MultiObjectiveHammingBallHillClimberStatistics getStatistics() {
        return statistics;
    }
    
    
    public double getSubFunctionEvaluation(int subFunction) {
        return subfnsEvals[subFunction];
    }
    
    
    public double [] getMoveImprovementByID(int id) {
        return movesSelector.getMoveByID(id).getImprovement();
    }

}
