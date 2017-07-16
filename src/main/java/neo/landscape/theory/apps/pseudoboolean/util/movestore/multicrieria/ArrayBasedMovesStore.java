package neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria;

import java.util.Map;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.util.Adaptor;
import neo.landscape.theory.apps.util.IteratorFromArray;

public class ArrayBasedMovesStore<M> implements MultiCriteriaMovesStore<M> {
    
    private ArrayBasedMove<M> [] mos;
    private ArrayBasedMove<M> [][][] scores;
    private int [][][] bucketIndices;
    private Random rnd;
    private ArrayBasedMoveFactory<M> movesFactory;
    private Adaptor<M, ArrayBasedMove<M>> adaptor = 
            new Adaptor<M, ArrayBasedMove<M>>() {
                @Override
                public M adapt(ArrayBasedMove<M> object) {
                    return object.getMove();
                }};

    public ArrayBasedMovesStore(ArrayBasedMoveFactory<M> movesFactory, int radius, int [] buckets, Map<SetOfVars, Integer> minimalPerfectHash, long seed) {
        this(movesFactory, radius, buckets, minimalPerfectHash, seed, new int [buckets.length]);
    }
    
    public ArrayBasedMovesStore(ArrayBasedMoveFactory<M> movesFactory, int radius, int [] buckets, Map<SetOfVars, Integer> minimalPerfectHash, long seed, int [] defaultBucket) {
        int criteria = buckets.length;
        if (criteria < 1) {
            throw new IllegalArgumentException ("At least one criteria should be used");
        }
        
        bucketIndices = new int [criteria][radius + 1][];
        for (int r=1; r <= radius; r++) {
            for (int criterion=0; criterion < criteria; criterion++) {
                bucketIndices[criterion][r] = new int [buckets[criterion]+1];
            }
        }
        
        this.movesFactory = movesFactory;
        
        mos = new ArrayBasedMove[minimalPerfectHash.size()]; 
        for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
                .entrySet()) {
            SetOfVars sov = entry.getKey();
            ArrayBasedMove<M> e = movesFactory.createArrayBasedMove(sov);
            mos[entry.getValue()] = e;
            for (int criterion = 0; criterion < criteria; criterion++) {
                bucketIndices[criterion][sov.size()][defaultBucket[criterion]+1]++;
            }
        }
        
        scores = new ArrayBasedMove[criteria][radius + 1][];
        
        for (int r = 1; r <= radius; r++) {
            for (int criterion=0; criterion < criteria; criterion++) {
                scores[criterion][r] = new ArrayBasedMove[bucketIndices[criterion][r][defaultBucket[criterion]+1]];
                for(int j=defaultBucket[criterion]+2; j < bucketIndices[criterion][r].length; j++) {
                    bucketIndices[criterion][r][j] = bucketIndices[criterion][r][j-1]; 
                }
                bucketIndices[criterion][r][defaultBucket[criterion]+1] = 0;
            }
        }
        
        for (Map.Entry<SetOfVars, Integer> entry : minimalPerfectHash
                .entrySet()) {
            int r = entry.getKey().size();
            ArrayBasedMove<M> move = mos[entry.getValue()];
            for (int criterion=0; criterion < criteria; criterion++) {
                move.setIndex(criterion, bucketIndices[criterion][r][defaultBucket[criterion]+1]);
                scores[criterion][r][move.getIndex(criterion)] = move;
                bucketIndices[criterion][r][defaultBucket[criterion]+1]++;
            }
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
    public Iterable<M> iterableOverBucket(int criterion, int radius, int bucket) {
        return IteratorFromArray.iterable(scores[criterion][radius], bucketIndices[criterion][radius][bucket], bucketIndices[criterion][radius][bucket+1], adaptor);
    }

    @Override
    public int getNumberOfBuckets(int criterion, int radius) {
        return bucketIndices[criterion][radius].length-1;
    }

    @Override
    public void changeMoveBucketLIFO(int criterion, int radius, int oldBucket, int newBucket, M move) {
        ArrayBasedMove<M> myMove = (ArrayBasedMove<M>)move;
        moveToNewBucket(criterion, radius, oldBucket, newBucket, myMove);
        swapMoves(criterion, radius, myMove, bucketIndices[criterion][radius][newBucket]);
    }
    
    @Override
    public void changeMoveBucketFIFO(int criterion, int radius, int oldBucket, int newBucket, M move) {
        ArrayBasedMove<M> myMove = (ArrayBasedMove<M>)move;
        moveToNewBucket(criterion, radius, oldBucket, newBucket, myMove);
        swapMoves(criterion, radius, myMove, bucketIndices[criterion][radius][newBucket+1]-1);
    }

    private void moveToNewBucket(int criterion, int radius, int oldBucket, int newBucket,
            ArrayBasedMove<M> myMove) {
        while (oldBucket < newBucket) {
            moveUpInBucketList(criterion, radius, oldBucket, myMove);
            oldBucket++;
        }
        
        while (oldBucket > newBucket) {
            moveDownInBucketList(criterion, radius, oldBucket, myMove);
            oldBucket--;
        }
    }

    private void moveUpInBucketList(int criterion, int radius, int oldBucket, ArrayBasedMove<M> move) {
        int target = bucketIndices[criterion][radius][oldBucket+1]-1;
        swapMoves(criterion, radius, move, target);
        bucketIndices[criterion][radius][oldBucket+1]--;
    }
    
    private void moveDownInBucketList(int criterion, int radius, int oldBucket, ArrayBasedMove<M> move) {
        int target = bucketIndices[criterion][radius][oldBucket];
        swapMoves(criterion, radius, move, target);
        bucketIndices[criterion][radius][oldBucket]++;
    }

    private void swapMoves(int criterion, int radius, ArrayBasedMove<M> move, int target) {
        int source = move.getIndex(criterion);
        if (source != target) {
            scores[criterion][radius][source] = scores[criterion][radius][target];
            scores[criterion][radius][source].setIndex(criterion,source);

            scores[criterion][radius][target] = move;
            move.setIndex(criterion, target);
        }
    }

    @Override
    public boolean isBucketEmpty(int criterion, int radius, int bucket) {
        return bucketIndices[criterion][radius][bucket]==bucketIndices[criterion][radius][bucket+1];
    }

    @Override
    public M getDeterministicMove(int criterion, int radius, int bucket) {
        if (isBucketEmpty(criterion, radius, bucket)) {
            throw new NoImprovingMoveException();
        }
        return scores[criterion][radius][bucketIndices[criterion][radius][bucket]].getMove();
    }
    
    @Override
    public int getBucketOfMove(int criterion, M move) {
        ArrayBasedMove<M> myMove = (ArrayBasedMove<M>)move;
        int radius = movesFactory.getSetOfVars(myMove).size();
        int index = myMove.getIndex(criterion);
        int bucket=0;
        for (; bucketIndices[criterion][radius][bucket+1] <= index; bucket++);
        return bucket;
    }

    @Override
    public M getRandomMove(int criterion, int radius, int bucket) {
        return getRandomMove(criterion, radius, bucket, bucket);
    }
    
    @Override
    public M getRandomMove(int criterion, int radius, int fromBucket, int toBucket) {
        if (bucketIndices[criterion][radius][fromBucket]==bucketIndices[criterion][radius][toBucket+1]) {
            throw new NoImprovingMoveException();
        }
        int size = bucketIndices[criterion][radius][toBucket+1]-bucketIndices[criterion][radius][fromBucket];
        int randomIndex = bucketIndices[criterion][radius][fromBucket] + rnd.nextInt(size);
        return scores[criterion][radius][randomIndex].getMove();
    }

    @Override
    public int getNumberOfMoves() {
        return mos.length;
    }

    @Override
    public int sizeOfBucket(int criterion, int radius, int bucket) {
        return bucketIndices[criterion][radius][bucket+1]-bucketIndices[criterion][radius][bucket];
    }
    
    @Override
    public int getNumberOfMoves(int criterion, int radius) {
        return scores[criterion][radius].length;
    }

    @Override
    public void changeAllMovesToBucket(int criterion, int radius, int bucket) {
        int i=0;
        for (;i <= bucket; i++) {
            bucketIndices[criterion][radius][i] = 0;
        }
        for (; i < bucketIndices[criterion][radius].length; i++) {
            bucketIndices[criterion][radius][i] = scores[criterion][radius].length;
        }
    }

    
}
