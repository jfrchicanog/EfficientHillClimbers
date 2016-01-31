package neo.landscape.theory.apps.pseudoboolean.util.movestore;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

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
