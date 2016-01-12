package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedVectorPBMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.MovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class MultiObjectiveSelector extends MultiObjectiveAbstractMovesSelector<VectorPBMove> {
    
    public static enum KindOfMove {
        DISIMPROVING (DISIMPROVING_BUCKET), 
        STRONGLY_IMPROVING (STRONGLY_IMPROVING_BUCKET), 
        W_IMPROVING (WEAKLY_IMPROVING_BUCKET);
        
        private int bucket;
        
        KindOfMove(int bucket) {
            this.bucket = bucket;
        }
        
        public int getBucket() {return bucket;}

    };
    
    
    private static final int UNCLASSIFIED_BUCKET = 0;
    private static final int DISIMPROVING_BUCKET = 1;
    private static final int STRONGLY_IMPROVING_BUCKET = 2;
    private static final int WEAKLY_IMPROVING_BUCKET = 3;
    private static final int BUCKET_NUMBER = 4;

    private int minImpRadius;
    private int impBucket;
    private int radius;
    
    private double [] weights;
    
    private ArrayBasedMoveFactory<VectorPBMove> movesFactory;
    
    public MultiObjectiveSelector(boolean allowRandomMoves, int theRadius, Random random, Map<SetOfVars, Integer> perfectHash, double [] weights) {
        this.weights = weights.clone();
        checkWeights();
        movesFactory = new ArrayBasedVectorPBMoveFactory(weights.length);
        randomMoves = allowRandomMoves;
        Map<SetOfVars, Integer> map = perfectHash;
        radius = theRadius;
        movesStore = createMovesStore(radius, BUCKET_NUMBER, map, random);
    }
    
    private void checkWeights() {
        for (double weight: weights) {
            if (weight <= 0.0) {
                throw new RuntimeException ("All components of the weight vector must be positive");
            }
        }
    }

    private MovesStore<VectorPBMove> createMovesStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, Random rnd) {
        long seed = rnd.nextLong();
        return new ArrayBasedMovesStore<VectorPBMove>(movesFactory, radius, buckets, minimalPerfectHash, seed, UNCLASSIFIED_BUCKET);
    }
    
    @Override
    public VectorPBMove getMovement() {
        if (!searchRadiusAndBucket()) {
            throw new NoImprovingMoveException();
    	} else {
    		return determineMovement(minImpRadius, impBucket);
    	}
    }

    private boolean searchRadiusAndBucket() {
        minImpRadius = searchMinRadiusInBucket(STRONGLY_IMPROVING_BUCKET);
        if (minImpRadius >= 1) {
            impBucket = STRONGLY_IMPROVING_BUCKET;
        } else {
            minImpRadius = searchMinRadiusInBucket(WEAKLY_IMPROVING_BUCKET);
            impBucket = STRONGLY_IMPROVING_BUCKET;
        }
        return minImpRadius >= 1;
    }

    private int searchMinRadiusInBucket(int bucket) {
        for (minImpRadius=1; minImpRadius <= radius; minImpRadius++) {
            if (!movesStore.isBucketEmpty(minImpRadius, bucket)) {
                return minImpRadius;
            }
        }
        return -1;
    }

    @Override
    public Iterable<VectorPBMove> allMoves() {
        return movesStore.iterableOverMoves();
    }

    @Override
    public void checkCorrectPositionOfMovesInSelector() {
        for (int theRadius = 1; theRadius <= radius; theRadius++) {
            assert movesStore.isBucketEmpty(theRadius, UNCLASSIFIED_BUCKET);
    		for (int theBucket = 0; theBucket < movesStore.getNumberOfBuckets(theRadius); theBucket++) {
    			for (VectorPBMove move : movesStore.iterableOverBucket(theRadius, theBucket)) {
    				assert move.flipVariables.size() == theRadius;
    				assert classifyMove(move).getBucket()==theBucket;
    			}
    		}
    	}
    }

    @Override
    public void unclassifyMove(VectorPBMove move) {        
        movesStore.changeMoveBucketFIFO(move.flipVariables.size(), 
                movesStore.getBucketOfMove(move), 
                UNCLASSIFIED_BUCKET, 
                move);
    }

    @Override
    public void assignBucketsToUnclassifiedMoves() {
        for (int rad=1; rad <= radius; rad++) {
            while (!movesStore.isBucketEmpty(rad, UNCLASSIFIED_BUCKET)) {
                VectorPBMove move = movesStore.getDeterministicMove(rad, UNCLASSIFIED_BUCKET);
                KindOfMove kind = classifyMove(move);
                movesStore.changeMoveBucketFIFO(rad, UNCLASSIFIED_BUCKET, kind.getBucket(), move);
            }
        }
    }

    public KindOfMove classifyMove(VectorPBMove move) {
        boolean positive=false;
        boolean negative=false;
        double wScore=0.0;
        double [] improvement = move.getImprovement();
        
        for (int i = 0; i < improvement.length; i++) {
            wScore += weights[i] * improvement[i];
            positive |= (improvement[i] > 0.0);
            negative |= (improvement[i] < 0.0);
        }
        
        if (positive && !negative) {
            return KindOfMove.STRONGLY_IMPROVING;
        } else if (wScore > 0.0) {
            return KindOfMove.W_IMPROVING;
        } else {
            return KindOfMove.DISIMPROVING;
        }
    }




}