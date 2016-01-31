package neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class ArrayBasedStoreVectorPBMove extends VectorPBMove implements ArrayBasedMove<VectorPBMove> {
    private int [] indices;
    
    public ArrayBasedStoreVectorPBMove(int criteria, double [] deltas, SetOfVars variables) {
        super(deltas, variables);
        indices = new int [criteria];
    }

    @Override
    public int getIndex(int criterion) {
        return indices[criterion];
    }

    @Override
    public void setIndex(int criterion, int index) {
        this.indices[criterion] = index;
    }

    @Override
    public VectorPBMove getMove() {
        return this;
    }

}
