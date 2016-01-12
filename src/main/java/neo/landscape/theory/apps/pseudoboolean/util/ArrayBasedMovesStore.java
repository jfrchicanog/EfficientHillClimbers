package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.util.Adaptor;
import neo.landscape.theory.apps.util.IteratorFromArray;

public class ArrayBasedMovesStore<M> implements MovesStore<M> {
    
    private ArrayBasedMove<M> [] mos;
    private ArrayBasedMove<M> [][] scores;
    private int [][] bucketIndices;
    private Random rnd;
    private ArrayBasedMoveFactory<M> movesFactory;
    private Adaptor<M, ArrayBasedMove<M>> adaptor = 
            new Adaptor<M, ArrayBasedMove<M>>() {
                @Override
                public M adapt(ArrayBasedMove<M> object) {
                    return object.getMove();
                }};

    public ArrayBasedMovesStore(ArrayBasedMoveFactory<M> movesFactory, int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, long seed) {
        this(movesFactory, radius, buckets, minimalPerfectHash, seed, 0);
    }
    
    public ArrayBasedMovesStore(ArrayBasedMoveFactory<M> movesFactory, int radius, int buckets, Map<SetOfVars, Integer> minimalPerfectHash, long seed, int defaultBucket) {
        bucketIndices = new int [radius + 1][buckets+1];
        this.movesFactory = movesFactory;
        
        mos = new ArrayBasedMove[minimalPerfectHash.size()]; 
        for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
                .entrySet()) {
            SetOfVars sov = entry.getKey();
            ArrayBasedMove e = movesFactory.createArrayBasedMove(sov);
            mos[entry.getValue()] = e;
            bucketIndices[sov.size()][defaultBucket+1]++;
        }
        
        scores = new ArrayBasedMove[radius + 1][];
        
        for (int r = 1; r <= radius; r++) {
            scores[r] = new ArrayBasedMove[bucketIndices[r][defaultBucket+1]];
            for(int j=defaultBucket+2; j < bucketIndices[r].length; j++) {
                bucketIndices[r][j] = bucketIndices[r][j-1]; 
            }
            bucketIndices[r][defaultBucket+1] = 0;
        }
        
        for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
                .entrySet()) {
            int r = entry.getKey().size();
            ArrayBasedMove<M> move = mos[entry.getValue()];
            move.setIndex(bucketIndices[r][defaultBucket+1]);
            scores[r][move.getIndex()] = move;
            bucketIndices[r][defaultBucket+1]++;
        }
        rnd = new Random(seed);

    }

    
    @Override
    public Iterable<M> iterableOverMoves() {
        return IteratorFromArray.iterable(mos, adaptor);
    }

    @Override
    public M getMoveByID(int id) {
        return mos[id].getMove();
    }

    @Override
    public Iterable<M> iterableOverBucket(int radius, int bucket) {
        return IteratorFromArray.iterable(scores[radius], bucketIndices[radius][bucket], bucketIndices[radius][bucket+1], adaptor);
    }

    @Override
    public int getNumberOfBuckets(int radius) {
        return bucketIndices[radius].length-1;
    }

    @Override
    public void changeMoveBucketLIFO(int radius, int oldBucket, int newBucket, M move) {
        ArrayBasedMove<M> myMove = (ArrayBasedMove<M>)move;
        moveToNewBucket(radius, oldBucket, newBucket, myMove);
        swapMoves(radius, myMove, bucketIndices[radius][newBucket]);
    }

    private void moveToNewBucket(int radius, int oldBucket, int newBucket,
            ArrayBasedMove<M> myMove) {
        while (oldBucket < newBucket) {
            moveUpInBucketList(radius, oldBucket, myMove);
            oldBucket++;
        }
        
        while (oldBucket > newBucket) {
            moveDownInBucketList(radius, oldBucket, myMove);
            oldBucket--;
        }
    }

    private void moveUpInBucketList(int radius, int oldBucket, ArrayBasedMove<M> move) {
        int target = bucketIndices[radius][oldBucket+1]-1;
        swapMoves(radius, move, target);
        bucketIndices[radius][oldBucket+1]--;
    }
    
    private void moveDownInBucketList(int radius, int oldBucket, ArrayBasedMove<M> move) {
        int target = bucketIndices[radius][oldBucket];
        swapMoves(radius, move, target);
        bucketIndices[radius][oldBucket]++;
    }

    private void swapMoves(int radius, ArrayBasedMove<M> move, int target) {
        int source = move.getIndex();
        if (source != target) {
            scores[radius][source] = scores[radius][target];
            scores[radius][source].setIndex(source);

            scores[radius][target] = move;
            move.setIndex(target);
        }
    }

    @Override
    public void changeMoveBucketFIFO(int radius, int oldBucket, int newBucket, M move) {
        ArrayBasedMove<M> myMove = (ArrayBasedMove<M>)move;
        moveToNewBucket(radius, oldBucket, newBucket, myMove);
        swapMoves(radius, myMove, bucketIndices[radius][newBucket+1]-1);
    }

    @Override
    public boolean isBucketEmpty(int radius, int bucket) {
        return bucketIndices[radius][bucket]==bucketIndices[radius][bucket+1];
    }

    @Override
    public M getDeterministicMove(int radius, int bucket) {
        if (isBucketEmpty(radius, bucket)) {
            throw new NoImprovingMoveException();
        }
        return scores[radius][bucketIndices[radius][bucket]].getMove();
    }
    
    @Override
    public int getBucketOfMove(M move) {
        ArrayBasedMove<M> myMove = (ArrayBasedMove<M>)move;
        int radius = movesFactory.getSetOfVars(myMove).size();
        int index = myMove.getIndex();
        int bucket=0;
        for (; bucketIndices[radius][bucket+1] <= index; bucket++);
        return bucket;
    }

    @Override
    public M getRandomMove(int radius, int bucket) {
        return getRandomMove(radius, bucket, bucket);
    }
    
    @Override
    public M getRandomMove(int radius, int fromBucket, int toBucket) {
        if (bucketIndices[radius][fromBucket]==bucketIndices[radius][toBucket+1]) {
            throw new NoImprovingMoveException();
        }
        int size = bucketIndices[radius][toBucket+1]-bucketIndices[radius][fromBucket];
        int randomIndex = bucketIndices[radius][fromBucket] + rnd.nextInt(size);
        return scores[radius][randomIndex].getMove();
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
