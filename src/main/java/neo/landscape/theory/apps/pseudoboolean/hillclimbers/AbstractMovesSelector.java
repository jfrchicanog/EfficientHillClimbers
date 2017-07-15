package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import neo.landscape.theory.apps.pseudoboolean.util.movestore.MovesStore;

public abstract class AbstractMovesSelector<M> implements MovesSelector<M> {

    protected MovesStore<M> movesStore;
    protected boolean randomMoves;

    public AbstractMovesSelector() {
        super();
    }

    protected M determineMovement(int radius, int bucket) {
        if (randomMoves) {
            return movesStore.getRandomMove(radius, bucket);
        } else {
            return movesStore.getDeterministicMove(radius, bucket); 
        }
        
        
    }

    @Override
    public M getMoveByID(int id) {
        return movesStore.getMoveByID(id);
    }

    @Override
    public int getNumberOfMoves() {
        return movesStore.getNumberOfMoves();
    }

}