package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

public interface MovesStore {

    public Iterable<RBallPBMove> iterableOverMoves();
    public int getNumberOfMoves();
    public RBallPBMove getMoveByID(int entryIndex);
    
    public Iterable<RBallPBMove> iterableOverBucket(int radius, int bucket);
    public int getNumberOfBuckets(int radius);
    
    public void changeMoveBucketLIFO(int radius, int oldBucket, int newBucket, RBallPBMove move);
    public void changeMoveBucketFIFO(int radius, int oldBucket, int newBucket, RBallPBMove move);
    
    public boolean isBucketEmpty(int radius, int bucket);
    
    public RBallPBMove getDeterministicMove(int radius, int bucket);
    public RBallPBMove getRandomMove(int radius, int bucket);

}