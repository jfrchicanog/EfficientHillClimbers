package neo.landscape.theory.apps.pseudoboolean.util.movestore;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

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
