package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Arrays;
import java.util.Map;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.linkedlist.DefaultEntryFactory;
import neo.landscape.theory.apps.util.linkedlist.DoubleLinkedList;
import neo.landscape.theory.apps.util.linkedlist.Entry;
import neo.landscape.theory.apps.util.linkedlist.EntryFactory;

public class DoubleLinkedListBasedStore {
    
    private Entry<RBallPBMove>[] mos;
    private DoubleLinkedList<RBallPBMove>[][] scores;
    private EntryFactory<RBallPBMove> entryFactory;

    public DoubleLinkedListBasedStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash) {
        entryFactory = new DefaultEntryFactory<RBallPBMove>();
        initializeMovesStore(buckets, radius, minimalPerfectHash);
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
    		Entry<RBallPBMove> e = entryFactory.getEntry(rmove);
    		mos[entry.getValue()] = e;
    		scores[sov.size()][0].add(e);
    	}
    }

    public void initializeScores(int radius, int buckets) {
        scores = new DoubleLinkedList[radius + 1][buckets];
        for (int i = 1; i <= radius; i++) {
    		for (int j = 0; j < buckets; j++) {
    			scores[i][j] = new DoubleLinkedList<RBallPBMove>(entryFactory);
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

    public Iterable<RBallPBMove> iterableOverBucket(int radius, int bucket) {
        return scores[radius][bucket];
    }

    public boolean isBucketEmpty(int radius, int bucket) {
        return scores[radius][bucket].isEmpty();
    }

    public int getNumberOfBuckets(int p) {
        return scores[p].length;
    }

    public Entry<RBallPBMove> getDeterministicMoveInBucket(int radius, int bucket) {
        return scores[radius][bucket].getFirst();
    }

    public void initializeMovesStore(int buckets, int radius, Map<SetOfVars, Integer> minimalPerfectHash) {
        initializeScores(radius, buckets);
    	initializeMovesArray(minimalPerfectHash);
    }
}


