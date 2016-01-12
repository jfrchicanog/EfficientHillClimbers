package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

public interface MovesSelector<M> {

    public M getMoveByID(int id);
    public M getMovement();
    public Iterable<M> allMoves();
    public int getNumberOfMoves();
    public void changeScoreBucket(M move, double oldValue, double newValue);
    public void checkCorrectPositionOfMovesInSelector();

}
