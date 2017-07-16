package neo.landscape.theory.apps.pseudoboolean.util.movestore.multicriteria;

import java.util.HashMap;
import java.util.Map;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.ArrayBasedMoveFactory;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.ArrayBasedMovesStore;
import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.ArrayBasedVectorPBMoveFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ArrayBasedMovesStoreTest {
    
    private SetOfVars [] sovs; 
    private ArrayBasedMovesStore<VectorPBMove> movesStore;
    private int [] buckets;
    private static final long seed = 0;
    
    @Before
    public void setup() {
        sovs = new SetOfVars[] {
        SetOfVars.immutable(1, 2),
        SetOfVars.immutable(1, 4),
        SetOfVars.immutable(2, 5),
        SetOfVars.immutable(1),
        SetOfVars.immutable(2),
        SetOfVars.immutable(3),
        SetOfVars.immutable(4),
        SetOfVars.immutable(5),
        SetOfVars.immutable(1,2,3),
        SetOfVars.immutable(2,3,4)
        };
        
        Map<SetOfVars, Integer> map = new HashMap<SetOfVars, Integer>();
        for (int i = 0; i < sovs.length; i++) {
            map.put(sovs[i], i);
        }
        
        int criteria = 2; 
        int dimension = 3;
        int radius = 3;
        buckets = new int [] {3, 4};
        ArrayBasedMoveFactory<VectorPBMove> movesFactory = new ArrayBasedVectorPBMoveFactory(criteria, dimension);
        movesStore = new ArrayBasedMovesStore<VectorPBMove>(movesFactory, radius, buckets, map, seed);
        
    }

    @Test
    public void testIterableOverMoves() {
        Assert.assertNotNull(movesStore);
        for (VectorPBMove move : movesStore.iterableOverMoves()) {
            checkIsIncluded(move);
        }
    }

    private void checkIsIncluded(VectorPBMove move) {
        for (SetOfVars setOfVars : sovs) {
            if (move.flipVariables == setOfVars) {
                return;
            }
        }
        Assert.fail("The move is not in the list of moves");
        
    }

    @Test
    public void testGetMoveByID() {
        for (int i = 0; i < sovs.length; i++) {
            Assert.assertSame(sovs[i], movesStore.getMoveByID(i).flipVariables);
        }
    }

    @Test
    public void testIterableOverBucket() {
        checkNumberOfMovesInBucket(0, 1, 0, 5);
        checkNumberOfMovesInBucket(0, 1, 1, 0);
        checkNumberOfMovesInBucket(0, 1, 2, 0);
        
        checkNumberOfMovesInBucket(1, 2, 0, 3);
        checkNumberOfMovesInBucket(1, 3, 0, 2);
    }

    @Test
    public void testGetNumberOfBuckets() {
        for (int r=1; r <= 3; r++) {
            for (int criterion=0; criterion < buckets.length; criterion++) {
                Assert.assertEquals(buckets[criterion], movesStore.getNumberOfBuckets(criterion, r));
            }
        }
    }

    @Test
    public void testChangeMoveBucketLIFO() {
        checkChangeMoveBucketLIFO(0);
        checkChangeMoveBucketLIFO(1);
    }
    public void checkChangeMoveBucketLIFO(int criterion) {
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 2, movesStore.getMoveByID(1));
        
        checkNumberOfMovesInBucket(criterion, 1, 0, 3);
        checkNumberOfMovesInBucket(criterion, 1, 1, 1);
        checkNumberOfMovesInBucket(criterion, 1, 2, 1);
        
        checkNumberOfMovesInBucket(criterion, 2, 0, 1);
        checkNumberOfMovesInBucket(criterion, 2, 1, 1);
        checkNumberOfMovesInBucket(criterion, 2, 2, 1);
        
        movesStore.changeMoveBucketFIFO(criterion, 1, 2, 1, movesStore.getMoveByID(4));
        checkNumberOfMovesInBucket(criterion, 1, 1, 2);
        checkNumberOfMovesInBucket(criterion, 1, 2, 0);
        
    }
    
    private void checkNumberOfMovesInBucket(int criterion, int radius, int bucket, int size) {
        int count=0;
        for(VectorPBMove move: movesStore.iterableOverBucket(criterion, radius, bucket)) {
            Assert.assertEquals(radius, move.flipVariables.size());
            count++;
        }
        Assert.assertEquals(size,  count);
        Assert.assertEquals(size, movesStore.sizeOfBucket(criterion, radius, bucket));
    }

    @Test
    public void testIsBucketEmpty() {
        movesStore.isBucketEmpty(0, 1, 1);
        movesStore.isBucketEmpty(0, 1, 2);
        movesStore.isBucketEmpty(0, 2, 1);
        movesStore.isBucketEmpty(0, 2, 2);
        movesStore.isBucketEmpty(0, 3, 1);
        movesStore.isBucketEmpty(0, 3, 2);
    }

    @Test
    public void testGetDeterministicMove(){
        checkGetDeterministicMove(0);
        checkGetDeterministicMove(1);
    }
    
    public void checkGetDeterministicMove(int criterion) {
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 2, movesStore.getMoveByID(1));
        
        for(int r=1; r <= 3; r++) {
            VectorPBMove move = movesStore.getDeterministicMove(criterion, r, 0);
            Assert.assertEquals(r, move.flipVariables.size());
        }
    }
    
    @Test(expected=NoImprovingMoveException.class)
    public void testGetDeterministicMoveException0() {
        checkGetDeterministicMoveException(0);
    }
    
    @Test(expected=NoImprovingMoveException.class)
    public void testGetDeterministicMoveException1() {
        checkGetDeterministicMoveException(1);
    }
    
    public void checkGetDeterministicMoveException(int criterion) {
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 2, movesStore.getMoveByID(1));
        
        VectorPBMove move = movesStore.getDeterministicMove(criterion, 3, 1);
    }

    @Test
    public void testGetRandomMove() {
        checkGetRandomMove(0);
        checkGetRandomMove(1);
    }
    
    public void checkGetRandomMove(int criterion) {
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(criterion, 1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(criterion, 2, 0, 2, movesStore.getMoveByID(1));
        
        for(int r=1; r <= 3; r++) {
            VectorPBMove move = movesStore.getRandomMove(criterion, r, 0);
            Assert.assertEquals(r, move.flipVariables.size());
        }
    }


}
