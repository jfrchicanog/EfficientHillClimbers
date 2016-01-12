package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;

public class ArrayBasedVectorPBMoveFactory implements ArrayBasedMoveFactory<VectorPBMove>{

    private int dimension;
    public ArrayBasedVectorPBMoveFactory(int dimension) {
        this.dimension = dimension;
    }
    
    @Override
    public ArrayBasedMove<VectorPBMove> createArrayBasedMove(SetOfVars sov) {
        return new ArrayBasedStoreVectorPBMove(new double [dimension], sov);
    }

    @Override
    public SetOfVars getSetOfVars(ArrayBasedMove<VectorPBMove> move) {
        return ((ArrayBasedStoreVectorPBMove)move).flipVariables;        
    }
}
