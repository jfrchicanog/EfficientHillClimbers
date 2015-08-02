package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

public interface MovesStore {

    public Iterable<RBallPBMove> iterableOverMoves();
    public RBallPBMove getMoveByID(int id);
    
    public Iterable<RBallPBMove> iterableOverMovesOfRadius(int radius);
    public void changeMoveBucket (int radius, int initialBucket, int finalBucket, RBallPBMove move);
    public int bucketSize(int radius, int bucket);
    public boolean isBucketEmpty(int radius, int bucket);
    public RBallPBMove getDeterministicMove(int radius, int bucket);
    public RBallPBMove getRandomMove(int radius, int bucket);
}
