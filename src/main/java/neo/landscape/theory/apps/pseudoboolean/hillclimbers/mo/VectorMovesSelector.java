package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

public interface VectorMovesSelector<M> {

    public M getMoveByID(int id);
    public M getMovement();
    public Iterable<M> allMoves();
    public int getNumberOfMoves();
    public void unclassifyMove(M move);
    public void assignBucketsToUnclassifiedMoves();
    public void checkCorrectPositionOfMovesInSelector();

}
