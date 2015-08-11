package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

public interface MovesSelector {

    public RBallPBMove getMoveByID(int id);

    public RBallPBMove getMovement();

    public Iterable<RBallPBMove> allMoves();
    public int getNumberOfMoves();
    public void changeScoreBucket(RBallPBMove move, double oldValue, double newValue);

    public void checkCorrectPositionOfMovesInSelector();

}