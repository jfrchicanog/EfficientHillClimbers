package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;

public class ArrayBasedStoreVectorPBMove extends VectorPBMove implements ArrayBasedMove<VectorPBMove> {
    private int index;
    
    public ArrayBasedStoreVectorPBMove(double [] deltas, SetOfVars variables) {
        super(deltas, variables);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public VectorPBMove getMove() {
        return this;
    }

}
