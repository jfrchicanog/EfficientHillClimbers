package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.ArrayBasedMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.ArrayBasedRBallPBMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.MovesStore;

public class NeutralSelector extends AbstractMovesSelector<RBallPBMove> {
    
    private int radius;
    private double neutralMaxProbability;
    private Random rnd;
    private ArrayBasedMoveFactory<RBallPBMove> movesFactory;
    
    private static final int DISIMPROVING_BUCKET=0;
    private static final int NEUTRAL_BUCKET=1;
    private static final int IMPROVING_BUCKET=2;
    
    public NeutralSelector(long seed, double maxNeutralProbability, Random random, int theRadius, Map<SetOfVars, Integer> perfectHash) {
        randomMoves = true;
        Map<SetOfVars, Integer> map = perfectHash;
        radius = theRadius;
        int buckets = 3;
        movesStore = createMovesStore(radius, buckets, map, random);
        rnd = new Random(seed);
        neutralMaxProbability = maxNeutralProbability;
        movesFactory = new ArrayBasedRBallPBMoveFactory();
    }
    
    private MovesStore<RBallPBMove> createMovesStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, Random rnd) {
        //return new DoubleLinkedListBasedStore(radius, buckets, minimalPerfectHash);
        long seed = rnd.nextLong();
        //System.out.println("Otra: "+seed);
        return new ArrayBasedMovesStore<RBallPBMove>(movesFactory, radius, buckets, minimalPerfectHash, seed, NEUTRAL_BUCKET);
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesSelector#getMovement()
     */
    @Override
    public RBallPBMove getMovement() {
        int rNeutral = searchRadius(NEUTRAL_BUCKET);
        int rImproving = searchRadius(IMPROVING_BUCKET);
        if (rNeutral==0 && rImproving==0) {
            throw new NoImprovingMoveException();
        } else {
            int neutralMoves = (rNeutral==0)?0:movesStore.sizeOfBucket(rNeutral, NEUTRAL_BUCKET);
            int improvingMoves = (rImproving==0)?0:movesStore.sizeOfBucket(rImproving, IMPROVING_BUCKET);
            if (improvingMoves == 0 
                    || neutralMoves == 0
                    || neutralMoves <= neutralMaxProbability * (improvingMoves+neutralMoves)) {
                if (rNeutral==rImproving) {
                    return movesStore.getRandomMove(rNeutral, NEUTRAL_BUCKET, IMPROVING_BUCKET);
                } else {
                    int selected = rnd.nextInt(improvingMoves+neutralMoves);
                    if (selected < improvingMoves) {
                        return movesStore.getRandomMove(rImproving, IMPROVING_BUCKET);
                    } else {
                        return movesStore.getRandomMove(rNeutral, NEUTRAL_BUCKET);
                    }
                }
            } else {
                if (rnd.nextDouble() < neutralMaxProbability) {
                    return movesStore.getRandomMove(rNeutral, NEUTRAL_BUCKET);
                } else {
                    return movesStore.getRandomMove(rImproving, IMPROVING_BUCKET);
                }
            }
    	}
    }

    private int searchRadius(int bucket) {
        for (int r=1; r <= radius; r++) {
            if (!movesStore.isBucketEmpty(r, bucket)) {
                return radius;
            }
        }
        return 0;
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
        int oldQualityIndex = getBucketIndex(oldValue);
        int newQualityIndex = getBucketIndex(newValue);
        
        if (oldQualityIndex != newQualityIndex) {
        	int p = move.flipVariables.size();
        	movesStore.changeMoveBucketLIFO(p, oldQualityIndex, newQualityIndex, move);
        }
    }

    private int getBucketIndex(double oldValue) {
        if (oldValue < 0) {
            return DISIMPROVING_BUCKET;
        } else if (oldValue == 0) {
            return NEUTRAL_BUCKET;
        } else {
            return IMPROVING_BUCKET;
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
    
    				if (q == DISIMPROVING_BUCKET) {
    					assert move.improvement < 0;
    				} else if (q == NEUTRAL_BUCKET) {
    					assert move.improvement == 0;
    				} else {
    					assert (move.improvement > 0);
    				}
    			}
    		}
    	}
    }



}