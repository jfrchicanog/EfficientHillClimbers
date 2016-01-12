package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.HashMap;
import java.util.Map;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.NoImprovingMoveException;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ArrayBasedMovesStoreTest {
    
    private SetOfVars [] sovs; 
    private ArrayBasedMovesStore<RBallPBMove> movesStore;
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
        
        ArrayBasedMoveFactory<RBallPBMove> movesFactory = new ArrayBasedRBallPBMoveFactory();
        movesStore = new ArrayBasedMovesStore<RBallPBMove>(movesFactory, 3, 3, map, seed);
        
    }

    @Test
    public void testIterableOverMoves() {
        Assert.assertNotNull(movesStore);
        for (RBallPBMove move : movesStore.iterableOverMoves()) {
            checkIsIncluded(move);
        }
    }

    private void checkIsIncluded(RBallPBMove move) {
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
        checkNumberOfMovesInBucket(1, 0, 5);
        checkNumberOfMovesInBucket(1, 1, 0);
        checkNumberOfMovesInBucket(1, 2, 0);
        
        checkNumberOfMovesInBucket(2, 0, 3);
        checkNumberOfMovesInBucket(3, 0, 2);
    }

    @Test
    public void testGetNumberOfBuckets() {
        for (int r=0; r < 3; r++) {
            Assert.assertEquals(3, movesStore.getNumberOfBuckets(r));
        }
    }

    @Test
    public void testChangeMoveBucketLIFO() {
        movesStore.changeMoveBucketFIFO(1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(2, 0, 2, movesStore.getMoveByID(1));
        
        checkNumberOfMovesInBucket(1, 0, 3);
        checkNumberOfMovesInBucket(1, 1, 1);
        checkNumberOfMovesInBucket(1, 2, 1);
        
        checkNumberOfMovesInBucket(2, 0, 1);
        checkNumberOfMovesInBucket(2, 1, 1);
        checkNumberOfMovesInBucket(2, 2, 1);
        
        movesStore.changeMoveBucketFIFO(1, 2, 1, movesStore.getMoveByID(4));
        checkNumberOfMovesInBucket(1, 1, 2);
        checkNumberOfMovesInBucket(1, 2, 0);
        
    }
    
    private void checkNumberOfMovesInBucket(int radius, int bucket, int size) {
        int count=0;
        for(RBallPBMove move: movesStore.iterableOverBucket(radius, bucket)) {
            Assert.assertEquals(radius, move.flipVariables.size());
            count++;
        }
        Assert.assertEquals(size,  count);
        Assert.assertEquals(size, movesStore.sizeOfBucket(radius, bucket));
    }

    @Test
    public void testIsBucketEmpty() {
        movesStore.isBucketEmpty(1, 1);
        movesStore.isBucketEmpty(1, 2);
        movesStore.isBucketEmpty(2, 1);
        movesStore.isBucketEmpty(2, 2);
        movesStore.isBucketEmpty(3, 1);
        movesStore.isBucketEmpty(3, 2);
    }

    @Test
    public void testGetDeterministicMove() {
        movesStore.changeMoveBucketFIFO(1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(2, 0, 2, movesStore.getMoveByID(1));
        
        for(int r=1; r <= 3; r++) {
            RBallPBMove move = movesStore.getDeterministicMove(r, 0);
            Assert.assertEquals(r, move.flipVariables.size());
        }
    }
    
    @Test(expected=NoImprovingMoveException.class)
    public void testGetDeterministicMoveException() {
        movesStore.changeMoveBucketFIFO(1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(2, 0, 2, movesStore.getMoveByID(1));
        
        RBallPBMove move = movesStore.getDeterministicMove(3, 1);
    }

    @Test
    public void testGetRandomMove() {
        movesStore.changeMoveBucketFIFO(1, 0, 1, movesStore.getMoveByID(3));
        movesStore.changeMoveBucketFIFO(1, 0, 2, movesStore.getMoveByID(4));
        movesStore.changeMoveBucketFIFO(2, 0, 1, movesStore.getMoveByID(0));
        movesStore.changeMoveBucketFIFO(2, 0, 2, movesStore.getMoveByID(1));
        
        for(int r=1; r <= 3; r++) {
            RBallPBMove move = movesStore.getRandomMove(r, 0);
            Assert.assertEquals(r, move.flipVariables.size());
        }
    }


}
