package neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria;


public interface MultiCriteriaMovesStore<M> {

    public Iterable<M> iterableOverMoves();
    public int getNumberOfMoves();
    public M getMoveByID(int entryIndex);
    
    public Iterable<M> iterableOverBucket(int criterion, int radius, int bucket);
    public int getNumberOfBuckets(int criterion, int radius);
    
    public void changeMoveBucketLIFO(int criterion, int radius, int oldBucket, int newBucket, M move);
    public void changeMoveBucketFIFO(int criterion, int radius, int oldBucket, int newBucket, M move);
    
    public boolean isBucketEmpty(int criterion, int radius, int bucket);
    public int sizeOfBucket(int criterion, int radius, int bucket);
    
    public M getDeterministicMove(int criterion, int radius, int bucket);
    public M getRandomMove(int criterion, int radius, int bucket);
    public M getRandomMove(int criterion, int radius, int fromBucket, int toBucket);
    public abstract int getBucketOfMove(int criterion, M move);

}
