package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Map;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.IteratorFromArray;
import neo.landscape.theory.apps.util.linkedlist.DoubleLinkedList;
import neo.landscape.theory.apps.util.linkedlist.Entry;
import neo.landscape.theory.apps.util.linkedlist.EntryFactory;

public class DoubleLinkedListBasedStore implements MovesStore {
    
    private MemoryEfficientEntryRBallPBMove [] mos;
    private DoubleLinkedList<RBallPBMove>[][] scores;
    private EntryFactory<RBallPBMove> entryFactory;

    public DoubleLinkedListBasedStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash) {
        createEntryFactory();
        initializeScores(radius, buckets);
        initializeMovesArray(minimalPerfectHash);
    }

    private void createEntryFactory() {
        entryFactory = new MemoryEfficientEntryFactoryRBallPBMove();
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#iterableOverMoves()
     */
    @Override
    public Iterable<RBallPBMove> iterableOverMoves() {
        return IteratorFromArray.iterable(mos);
    }
    
    private void initializeScores(int radius, int buckets) {
        scores = new DoubleLinkedList[radius + 1][buckets];
        for (int i = 1; i <= radius; i++) {
            for (int j = 0; j < buckets; j++) {
                scores[i][j] = new DoubleLinkedList<RBallPBMove>(entryFactory);
            }
        }
    }
    
    private void initializeMovesArray(Map<SetOfVars, Integer> minimalPerfectHash) {
        mos = new MemoryEfficientEntryRBallPBMove[minimalPerfectHash.size()];
    
    	for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
    			.entrySet()) {
    		SetOfVars sov = entry.getKey();
    		MemoryEfficientEntryRBallPBMove e = createMove(0, sov);
    		mos[entry.getValue()] = e;
    		scores[sov.size()][0].add((Entry<RBallPBMove>) e);
    	}
    }

    private MemoryEfficientEntryRBallPBMove createMove(int improvement, SetOfVars sov) {
        return new MemoryEfficientEntryRBallPBMove(improvement, sov);
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#changeMoveBucketLIFO(int, int, int, neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove)
     */
    @Override
    public void changeMoveBucketLIFO(int radius, int oldBucket, int newBucket, RBallPBMove move) {
        scores[radius][oldBucket].remove((MemoryEfficientEntryRBallPBMove)move);
        scores[radius][newBucket].add((Entry<RBallPBMove>)move);
    }
    
    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#changeMoveBucketFIFO(int, int, int, neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove)
     */
    @Override
    public void changeMoveBucketFIFO(int radius, int oldBucket, int newBucket, RBallPBMove move) {
        scores[radius][oldBucket].remove((MemoryEfficientEntryRBallPBMove)move);
        scores[radius][newBucket].append((MemoryEfficientEntryRBallPBMove)move);
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#getMoveByID(int)
     */
    @Override
    public RBallPBMove getMoveByID(int id) {
        return mos[id];
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#iterableOverBucket(int, int)
     */
    @Override
    public Iterable<RBallPBMove> iterableOverBucket(int radius, int bucket) {
        return scores[radius][bucket];
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#isBucketEmpty(int, int)
     */
    @Override
    public boolean isBucketEmpty(int radius, int bucket) {
        return scores[radius][bucket].isEmpty();
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#getNumberOfBuckets(int)
     */
    @Override
    public int getNumberOfBuckets(int radius) {
        return scores[radius].length;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.pseudoboolean.util.MovesStoredIface#getDeterministicMoveInBucket(int, int)
     */
    @Override
    public RBallPBMove getDeterministicMove(int radius, int bucket) {
        return (RBallPBMove)scores[radius][bucket].getFirst();
    }

    @Override
    public RBallPBMove getRandomMove(int radius, int bucket) {
        throw new UnsupportedOperationException();
    }

}


