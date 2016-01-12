package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedRBallPBMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.MovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class QualityBasedNonNeutralSelector extends AbstractMovesSelector<RBallPBMove> {
    
    private int minImpRadius;
    private int minImpBucket;
    private int radius;
    private boolean lifo;
    private double[] qualityLimits;
    private ArrayBasedMoveFactory<RBallPBMove> movesFactory;
    
    public QualityBasedNonNeutralSelector(boolean allowRandomMoves, int theRadius, double[] theQualityLimits, Random random, boolean isLifo, Map<SetOfVars, Integer> perfectHash) {
        movesFactory = new ArrayBasedRBallPBMoveFactory();
        randomMoves = allowRandomMoves;
        Map<SetOfVars, Integer> map = perfectHash;
        radius = theRadius;
        qualityLimits = theQualityLimits;
        int buckets = 2 + ((qualityLimits == null) ? 0 : qualityLimits.length);
        
        movesStore = createMovesStore(radius, buckets, map, random);

        lifo = isLifo;
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
        } else if (qualityLimits == null) {
            return 1;
        } else {
            int i = 0;
            for (i = 0; i < qualityLimits.length && qualityLimits[i] <= val; i++)
                ;
            return i + 1;
        }
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
    					if (qualityLimits != null) {
    						assert move.improvement < qualityLimits[0];
    					}
    				} else {
    					assert (move.improvement >= qualityLimits[q - 2]);
    					if (q <= qualityLimits.length) {
    						assert move.improvement < qualityLimits[q - 1];
    					}
    				}
    			}
    		}
    	}
    }



}