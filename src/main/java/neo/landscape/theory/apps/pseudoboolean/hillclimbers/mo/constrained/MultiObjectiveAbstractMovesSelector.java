package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained;

import neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria.MultiCriteriaMovesStore;

public abstract class MultiObjectiveAbstractMovesSelector<M> implements ConstrainedVectorMovesSelector<M> {

    protected MultiCriteriaMovesStore<M> movesStore;
    protected boolean randomMoves;

    public MultiObjectiveAbstractMovesSelector() {
        super();
    }

    protected M determineMovement(int criterion, int radius, int bucket) {
        if (randomMoves) {
            return movesStore.getRandomMove(criterion, radius, bucket);
        } else {
            return movesStore.getDeterministicMove(criterion, radius, bucket); 
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