package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.util.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.MovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class DeterministicQualityBasedNonNeutralSelector {
    public MovesStore movesStore;
    public int[] maxNonEmptyScore;
    private int minImpRadius;
    private int radius;
    private boolean lifo;
    private double[] quality_limits;

    public DeterministicQualityBasedNonNeutralSelector(RBallEfficientHillClimberSnapshot rBallSnapshot) {
        Map<SetOfVars, Integer> map = rBallSnapshot.rballfio.minimalPerfectHash;
        radius = rBallSnapshot.rball.radius;
        minImpRadius = radius + 1;
        quality_limits = rBallSnapshot.rball.quality_limits;
        int buckets = 2 + ((quality_limits == null) ? 0 : quality_limits.length);
        
        movesStore = createMovesStore(radius, buckets, map, rBallSnapshot.rnd);
        initializeMaxScores();
        for (int i = 1; i <= radius; i++) {
            maxNonEmptyScore[i] = 0;
        }
        lifo = rBallSnapshot.rball.lifo;
    }
    
    private MovesStore createMovesStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, Random rnd) {
        //return new DoubleLinkedListBasedStore(radius, buckets, minimalPerfectHash);
        long seed = rnd.nextLong();
        //System.out.println("Otra: "+seed);
        return new ArrayBasedMovesStore(radius, buckets, minimalPerfectHash, seed);
    }

    private void initializeMaxScores() {
        maxNonEmptyScore = new int[radius + 1];
        for (int i = 1; i <= radius; i++) {
            maxNonEmptyScore[i] = 0;
        }
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

    public RBallPBMove getMoveByID(int id) {
        return movesStore.getMoveByID(id);
    }

    public RBallPBMove getMovementFromSelector() {
        updateInternalDataStructures();
        if (minImpRadius > radius) {
            throw new NoImprovingMoveException();
    	} else {
    		return movesStore.getDeterministicMove(minImpRadius, maxNonEmptyScore[minImpRadius]);
    	}
    }

    

    public Iterable<RBallPBMove> allMoves() {
        return movesStore.iterableOverMoves();
    }

    private void updateInternalDataStructures() {
        minImpRadius = radius + 1;
        for (int i = radius; i >= 1; i--) {
            while (maxNonEmptyScore[i] > 0
                    && movesStore.isBucketEmpty(i, maxNonEmptyScore[i])) {
                maxNonEmptyScore[i]--;
            }
            if (maxNonEmptyScore[i] > 0) {
                minImpRadius = i;
            }
        }
    }

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
        
        	if (newQualityIndex > maxNonEmptyScore[p]) {
        		// It is an improving move (necessarily, because
        		// maxNomEmptyScore is 0 at least
        		maxNonEmptyScore[p] = newQualityIndex;
        	}
        }
    }



}