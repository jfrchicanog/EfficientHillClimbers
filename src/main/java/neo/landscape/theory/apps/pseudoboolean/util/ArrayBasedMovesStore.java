package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.util.IteratorFromArray;

public class ArrayBasedMovesStore implements MovesStore {
    
    private ArrayBasedStoreRBallPBMove [] mos;
    private ArrayBasedStoreRBallPBMove [][] scores;
    private int [][] bucketIndices;
    private Random rnd;

    public ArrayBasedMovesStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, long seed) {
        this(radius, buckets, minimalPerfectHash, seed, 0);
    }
    
    public ArrayBasedMovesStore(int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, long seed, int defaultBucket) {
        bucketIndices = new int [radius + 1][buckets+1];
        
        mos = new ArrayBasedStoreRBallPBMove[minimalPerfectHash.size()]; 
        for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
                .entrySet()) {
            SetOfVars sov = entry.getKey();
            ArrayBasedStoreRBallPBMove e = createMove(0, sov);
            mos[entry.getValue()] = e;
            bucketIndices[sov.size()][defaultBucket+1]++;
        }
        
        scores = new ArrayBasedStoreRBallPBMove[radius + 1][];
        
        for (int r = 1; r <= radius; r++) {
            scores[r] = new ArrayBasedStoreRBallPBMove[bucketIndices[r][defaultBucket+1]];
            for(int j=defaultBucket+2; j < bucketIndices[r].length; j++) {
                bucketIndices[r][j] = bucketIndices[r][j-1]; 
            }
            bucketIndices[r][defaultBucket+1] = 0;
        }
        
        for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
                .entrySet()) {
            int r = entry.getKey().size();
            ArrayBasedStoreRBallPBMove move = mos[entry.getValue()];
            move.index = bucketIndices[r][defaultBucket+1];
            scores[r][move.index] = move;
            bucketIndices[r][defaultBucket+1]++;
        }
        rnd = new Random(seed);

    }
        
    private ArrayBasedStoreRBallPBMove createMove(int improvement, SetOfVars sov) {
        return new ArrayBasedStoreRBallPBMove(improvement, sov);
    }
    
    @Override
    public Iterable<RBallPBMove> iterableOverMoves() {
        return IteratorFromArray.iterable(mos);
    }

    @Override
    public RBallPBMove getMoveByID(int id) {
        return mos[id];
    }

    @Override
    public Iterable<RBallPBMove> iterableOverBucket(int radius, int bucket) {
        return IteratorFromArray.iterable(scores[radius], bucketIndices[radius][bucket], bucketIndices[radius][bucket+1]);
    }

    @Override
    public int getNumberOfBuckets(int radius) {
        return bucketIndices[radius].length-1;
    }

    @Override
    public void changeMoveBucketLIFO(int radius, int oldBucket, int newBucket, RBallPBMove move) {
        ArrayBasedStoreRBallPBMove myMove = (ArrayBasedStoreRBallPBMove)move;
        moveToNewBucket(radius, oldBucket, newBucket, myMove);
        swapMoves(radius, myMove, bucketIndices[radius][newBucket]);
    }

    private void moveToNewBucket(int radius, int oldBucket, int newBucket,
            ArrayBasedStoreRBallPBMove myMove) {
        while (oldBucket < newBucket) {
            moveUpInBucketList(radius, oldBucket, myMove);
            oldBucket++;
        }
        
        while (oldBucket > newBucket) {
            moveDownInBucketList(radius, oldBucket, myMove);
            oldBucket--;
        }
    }

    private void moveUpInBucketList(int radius, int oldBucket, ArrayBasedStoreRBallPBMove move) {
        int target = bucketIndices[radius][oldBucket+1]-1;
        swapMoves(radius, move, target);
        bucketIndices[radius][oldBucket+1]--;
    }
    
    private void moveDownInBucketList(int radius, int oldBucket, ArrayBasedStoreRBallPBMove move) {
        int target = bucketIndices[radius][oldBucket];
        swapMoves(radius, move, target);
        bucketIndices[radius][oldBucket]++;
    }

    private void swapMoves(int radius, ArrayBasedStoreRBallPBMove move, int target) {
        int source = move.index;
        if (source != target) {
            scores[radius][source] = scores[radius][target];
            scores[radius][source].index = source;

            scores[radius][target] = move;
            move.index = target;
        }
    }

    @Override
    public void changeMoveBucketFIFO(int radius, int oldBucket, int newBucket, RBallPBMove move) {
        ArrayBasedStoreRBallPBMove myMove = (ArrayBasedStoreRBallPBMove)move;
        moveToNewBucket(radius, oldBucket, newBucket, myMove);
        swapMoves(radius, myMove, bucketIndices[radius][newBucket+1]-1);
    }

    @Override
    public boolean isBucketEmpty(int radius, int bucket) {
        return bucketIndices[radius][bucket]==bucketIndices[radius][bucket+1];
    }

    @Override
    public RBallPBMove getDeterministicMove(int radius, int bucket) {
        if (isBucketEmpty(radius, bucket)) {
            throw new NoImprovingMoveException();
        }
        return scores[radius][bucketIndices[radius][bucket]];
    }

    @Override
    public RBallPBMove getRandomMove(int radius, int bucket) {
        return getRandomMove(radius, bucket, bucket);
    }
    
    @Override
    public RBallPBMove getRandomMove(int radius, int fromBucket, int toBucket) {
        if (bucketIndices[radius][fromBucket]==bucketIndices[radius][toBucket+1]) {
            throw new NoImprovingMoveException();
        }
        int size = bucketIndices[radius][toBucket+1]-bucketIndices[radius][fromBucket];
        int randomIndex = bucketIndices[radius][fromBucket] + rnd.nextInt(size);
        return scores[radius][randomIndex];
    }

    @Override
    public int getNumberOfMoves() {
        return mos.length;
    }

    

    @Override
    public int sizeOfBucket(int radius, int bucket) {
        return bucketIndices[radius][bucket+1]-bucketIndices[radius][bucket];
    }
    
}
