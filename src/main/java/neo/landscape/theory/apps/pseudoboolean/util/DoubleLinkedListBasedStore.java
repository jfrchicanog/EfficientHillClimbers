package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Arrays;
import java.util.Map;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.DoubleLinkedList;
import neo.landscape.theory.apps.util.DoubleLinkedList.Entry;

public class DoubleLinkedListBasedStore {
    private Entry<RBallPBMove>[] mos;
    private DoubleLinkedList<RBallPBMove>[][] scores;

    public DoubleLinkedListBasedStore() {
    }

    public Iterable<Entry<RBallPBMove>> iterableOverMoves() {
        return Arrays.asList(mos);
    }
    
    public void initializeMovesArray(Map<SetOfVars, Integer> minimalPerfectHash) {
        mos = new Entry[minimalPerfectHash.size()];
    
    	for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
    			.entrySet()) {
    		SetOfVars sov = entry.getKey();
    		RBallPBMove rmove = new RBallPBMove(0, sov);
    		Entry<RBallPBMove> e = new Entry<RBallPBMove>(rmove);
    		mos[entry.getValue()] = e;
    		scores[sov.size()][0].add(e);
    	}
    }

    public void initializeScores(int radius, int buckets) {
        scores = new DoubleLinkedList[radius + 1][buckets];
        for (int i = 1; i <= radius; i++) {
    		for (int j = 0; j < buckets; j++) {
    			scores[i][j] = new DoubleLinkedList<RBallPBMove>();
    		}
    	}
    }

    public void changeMoveBucketLIFO(int p, int oldQualityIndex, int newQualityIndex, Entry<RBallPBMove> e) {
        scores[p][oldQualityIndex].remove(e);
        scores[p][newQualityIndex].add(e);
    }
    
    public void changeMoveBucketFIFO(int p, int oldQualityIndex, int newQualityIndex, Entry<RBallPBMove> e) {
        scores[p][oldQualityIndex].remove(e);
        scores[p][newQualityIndex].append(e);
    }

    public Entry<RBallPBMove> getMoveByID(int entryIndex) {
        return mos[entryIndex];
    }

    public DoubleLinkedList<RBallPBMove> iterableOverBucket(int radius, int bucket) {
        return scores[radius][bucket];
    }

    public boolean isBucketEmpty(int radius, int bucket) {
        return scores[radius][bucket].isEmpty();
    }

    public int getNumberOfBuckets(int p) {
        return scores[p].length;
    }

    public Entry<RBallPBMove> getDeterministicMoveInBucket(int radius, int bucket) {
        return iterableOverBucket(radius, bucket).getFirst();
    }
}


