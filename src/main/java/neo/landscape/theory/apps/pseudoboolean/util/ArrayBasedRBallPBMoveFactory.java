package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

public class ArrayBasedRBallPBMoveFactory implements ArrayBasedMoveFactory<RBallPBMove>{

    @Override
    public ArrayBasedMove<RBallPBMove> createArrayBasedMove(SetOfVars sov) {
        return new ArrayBasedStoreRBallPBMove(0, sov);
    }

    @Override
    public SetOfVars getSetOfVars(ArrayBasedMove<RBallPBMove> move) {
        return ((ArrayBasedStoreRBallPBMove)move).flipVariables;        
    }


}
