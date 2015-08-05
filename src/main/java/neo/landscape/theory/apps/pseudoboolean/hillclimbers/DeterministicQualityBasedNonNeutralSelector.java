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

    public DeterministicQualityBasedNonNeutralSelector(RBallEfficientHillClimberSnapshot rBallSnapshot) {
        Map<SetOfVars, Integer> map = rBallSnapshot.rballfio.minimalPerfectHash;
        radius = rBallSnapshot.rball.radius;
        minImpRadius = radius + 1;
        int buckets = 2 + ((rBallSnapshot.rball.quality_limits == null) ? 0
                : rBallSnapshot.rball.quality_limits.length);
        
        movesStore = createMovesStore(radius, buckets, map, rBallSnapshot.rnd);
        initializeMaxScores();
        for (int i = 1; i <= radius; i++) {
            maxNonEmptyScore[i] = 0;
        }
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
    
    
    

    public RBallPBMove getMoveByID(int id) {
        return movesStore.getMoveByID(id);
    }

    public RBallPBMove getMovementFromSelector(RBallEfficientHillClimberSnapshot rBallEfficientHillClimberSnapshot, int r) {
        if (getMinImpRadius() > r) {
    		return new RBallPBMove(0, rBallEfficientHillClimberSnapshot.problem.getN());
    	} else {
    		return movesStore.getDeterministicMove(getMinImpRadius(), maxNonEmptyScore[getMinImpRadius()]);
    	}
    }

    public int getMinImpRadius() {
        return minImpRadius;
    }


    
    
    

    

    public Iterable<RBallPBMove> allMoves() {
        return movesStore.iterableOverMoves();
    }

    public void updateInternalDataStructures() {
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

    public void changeScoreBucket(RBallEfficientHillClimberSnapshot rBallSnapshot, RBallPBMove move, SetOfVars sov, double oldValue, double newValue) {
        int oldQualityIndex = rBallSnapshot.rball.getQualityIndex(oldValue);
        int newQualityIndex = rBallSnapshot.rball.getQualityIndex(newValue);
        
        if (oldQualityIndex != newQualityIndex) {
        	int p = sov.size();
        	if (rBallSnapshot.rball.lifo) {
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