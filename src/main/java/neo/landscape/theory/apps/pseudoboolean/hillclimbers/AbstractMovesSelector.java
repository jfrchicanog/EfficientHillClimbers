package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import neo.landscape.theory.apps.pseudoboolean.util.MovesStore;

public abstract class AbstractMovesSelector implements MovesSelector {

    protected MovesStore movesStore;
    protected boolean randomMoves;

    public AbstractMovesSelector() {
        super();
    }

    protected RBallPBMove determineMovement(int radius, int bucket) {
        if (randomMoves) {
            return movesStore.getRandomMove(radius, bucket);
        } else {
            return movesStore.getDeterministicMove(radius, bucket); 
        }
        
        
    }

    @Override
    public RBallPBMove getMoveByID(int id) {
        return movesStore.getMoveByID(id);
    }

}