package neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.VectorPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class ArrayBasedVectorPBMoveFactory implements ArrayBasedMoveFactory<VectorPBMove>{

    private int dimension;
    private int criteria;
    public ArrayBasedVectorPBMoveFactory(int criteria, int dimension) {
        this.dimension = dimension;
        this.criteria = criteria;
    }
    
    @Override
    public ArrayBasedMove<VectorPBMove> createArrayBasedMove(SetOfVars sov) {
        return new ArrayBasedStoreVectorPBMove(criteria, new double [dimension], sov);
    }

    @Override
    public SetOfVars getSetOfVars(ArrayBasedMove<VectorPBMove> move) {
        return ((ArrayBasedStoreVectorPBMove)move).flipVariables;        
    }
}
