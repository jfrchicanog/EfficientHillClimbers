package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Map;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.IteratorFromArray;
import neo.landscape.theory.apps.util.linkedlist.DoubleLinkedList;
import neo.landscape.theory.apps.util.linkedlist.Entry;
import neo.landscape.theory.apps.util.linkedlist.EntryFactory;

public class DoubleLinkedListBasedStore {
    
    private MemoryEfficientEntryRBallPBMove [] mos;
    private DoubleLinkedList<RBallPBMove>[][] scores;
    private EntryFactory<RBallPBMove> entryFactory;

    public DoubleLinkedListBasedStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash) {
        createEntryFactory();
        initializeScores(radius, buckets);
        initializeMovesArray(minimalPerfectHash);
    }

    public void createEntryFactory() {
        entryFactory = new MemoryEfficientEntryFactoryRBallPBMove();
    }

    public Iterable<RBallPBMove> iterableOverMoves() {
        return IteratorFromArray.iterable(mos);
    }
    
    public void initializeScores(int radius, int buckets) {
        scores = new DoubleLinkedList[radius + 1][buckets];
        for (int i = 1; i <= radius; i++) {
            for (int j = 0; j < buckets; j++) {
                scores[i][j] = new DoubleLinkedList<RBallPBMove>(entryFactory);
            }
        }
    }
    
    public void initializeMovesArray(Map<SetOfVars, Integer> minimalPerfectHash) {
        mos = new MemoryEfficientEntryRBallPBMove[minimalPerfectHash.size()];
    
    	for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
    			.entrySet()) {
    		SetOfVars sov = entry.getKey();
    		MemoryEfficientEntryRBallPBMove e = createMove(0, sov);
    		mos[entry.getValue()] = e;
    		scores[sov.size()][0].add((Entry<RBallPBMove>) e);
    	}
    }

    public MemoryEfficientEntryRBallPBMove createMove(int improvement, SetOfVars sov) {
        return new MemoryEfficientEntryRBallPBMove(improvement, sov);
    }

    public void changeMoveBucketLIFO(int p, int oldQualityIndex, int newQualityIndex, RBallPBMove e) {
        scores[p][oldQualityIndex].remove((MemoryEfficientEntryRBallPBMove)e);
        scores[p][newQualityIndex].add((Entry<RBallPBMove>)e);
    }
    
    public void changeMoveBucketFIFO(int p, int oldQualityIndex, int newQualityIndex, RBallPBMove e) {
        scores[p][oldQualityIndex].remove((MemoryEfficientEntryRBallPBMove)e);
        scores[p][newQualityIndex].append((MemoryEfficientEntryRBallPBMove)e);
    }

    public RBallPBMove getMoveByID(int entryIndex) {
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

    public RBallPBMove getDeterministicMoveInBucket(int radius, int bucket) {
        return (RBallPBMove)scores[radius][bucket].getFirst();
    }

}


