package neo.landscape.theory.apps.pseudoboolean.util;


public interface MovesStore<M> {

    public Iterable<M> iterableOverMoves();
    public int getNumberOfMoves();
    public M getMoveByID(int entryIndex);
    
    public Iterable<M> iterableOverBucket(int radius, int bucket);
    public int getNumberOfBuckets(int radius);
    
    public void changeMoveBucketLIFO(int radius, int oldBucket, int newBucket, M move);
    public void changeMoveBucketFIFO(int radius, int oldBucket, int newBucket, M move);
    
    public boolean isBucketEmpty(int radius, int bucket);
    public int sizeOfBucket(int radius, int bucket);
    
    public M getDeterministicMove(int radius, int bucket);
    public M getRandomMove(int radius, int bucket);
    public M getRandomMove(int radius, int fromBucket, int toBucket);
    public abstract int getBucketOfMove(M move);

}
