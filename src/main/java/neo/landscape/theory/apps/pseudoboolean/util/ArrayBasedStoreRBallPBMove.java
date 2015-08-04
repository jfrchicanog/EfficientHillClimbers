package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

public class ArrayBasedStoreRBallPBMove extends RBallPBMove {
    public int index;
    
    public ArrayBasedStoreRBallPBMove(double improvement, SetOfVars variables) {
        super(improvement, variables);
    }

}
