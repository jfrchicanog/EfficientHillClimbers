package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedRBallPBMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.MovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class DeterministicQualityBasedNonNeutralSelector extends AbstractMovesSelector<RBallPBMove> {
    
    private int minImpRadius;
    private int minImpBucket;
    private int radius;
    private boolean lifo;
    private double[] quality_limits;
    private ArrayBasedMoveFactory<RBallPBMove> movesFactory;
    
    
    public DeterministicQualityBasedNonNeutralSelector(RBallEfficientHillClimberSnapshot rBallSnapshot) {
        movesFactory = new ArrayBasedRBallPBMoveFactory();
        randomMoves = rBallSnapshot.rball.randomMoves;
        Map<SetOfVars, Integer> map = rBallSnapshot.rballfio.minimalPerfectHash;
        radius = rBallSnapshot.rball.radius;
        quality_limits = rBallSnapshot.rball.qualityLimits;
        int buckets = 2 + ((quality_limits == null) ? 0 : quality_limits.length);
        
        movesStore = createMovesStore(radius, buckets, map, rBallSnapshot.rnd);

        lifo = rBallSnapshot.rball.lifo;
    }
    
    private MovesStore<RBallPBMove> createMovesStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, Random rnd) {
        //return new DoubleLinkedListBasedStore(radius, buckets, minimalPerfectHash);
        long seed = rnd.nextLong();
        //System.out.println("Otra: "+seed);
        return new ArrayBasedMovesStore<RBallPBMove>(movesFactory, radius, buckets, minimalPerfectHash, seed);
    }

    private int getQualityIndex(double val) {
        if (val <= 0) {
            return 0;
        } else if (quality_limits == null) {
            return 1;
        } else {
            int i = 0;
            for (i = 0; i < quality_limits.length && quality_limits[i] <= val; i++)
                ;
            return i + 1;
        }
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector#getMoveByID(int)
     */
    @Override
    public RBallPBMove getMoveByID(int id) {
        return movesStore.getMoveByID(id);
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector#getMovement()
     */
    @Override
    public RBallPBMove getMovement() {
        if (!searchRadiusAndBucket()) {
            throw new NoImprovingMoveException();
    	} else {
    		return determineMovement(minImpRadius, minImpBucket);
    	}
    }

    private boolean searchRadiusAndBucket() {
        for (minImpRadius=1; minImpRadius <= radius; minImpRadius++) {
            minImpBucket = searchBucketInRadius(minImpRadius);
            if (minImpBucket >= 0) {
                return true;
            }
        }
        return false;
    }

    private int searchBucketInRadius(int radius) {
        for (int bucket=movesStore.getNumberOfBuckets(radius)-1; bucket >= 1; bucket--) {
            if (!movesStore.isBucketEmpty(radius, bucket)) {
                return bucket;
            }
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector#allMoves()
     */
    @Override
    public Iterable<RBallPBMove> allMoves() {
        return movesStore.iterableOverMoves();
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector#changeScoreBucket(neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove, double, double)
     */
    @Override
    public void changeScoreBucket(RBallPBMove move, double oldValue, double newValue) {
        int oldQualityIndex = getQualityIndex(oldValue);
        int newQualityIndex = getQualityIndex(newValue);
        
        if (oldQualityIndex != newQualityIndex) {
        	int p = move.flipVariables.size();
        	if (lifo) {
        	    movesStore.changeMoveBucketLIFO(p, oldQualityIndex, newQualityIndex, move);
        	} else {
        	    movesStore.changeMoveBucketFIFO(p, oldQualityIndex, newQualityIndex, move);
        	}
        }
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector#checkCorrectPositionOfMovesInSelector()
     */
    @Override
    public void checkCorrectPositionOfMovesInSelector() {
        for (int p = 1; p <= radius; p++) {
    		for (int q = 0; q < movesStore.getNumberOfBuckets(p); q++) {
    			for (RBallPBMove move : movesStore.iterableOverBucket(p, q)) {
    				assert move.flipVariables.size() == p;
    
    				if (q == 0) {
    					assert move.improvement <= 0;
    				} else if (q == 1) {
    					assert move.improvement > 0;
    					if (quality_limits != null) {
    						assert move.improvement < quality_limits[0];
    					}
    				} else {
    					assert (move.improvement >= quality_limits[q - 2]);
    					if (q <= quality_limits.length) {
    						assert move.improvement < quality_limits[q - 1];
    					}
    				}
    			}
    		}
    	}
    }

    @Override
    public int getNumberOfMoves() {
        return movesStore.getNumberOfMoves();
    }



}
