package neo.landscape.theory.apps.pseudoboolean.util;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

public class ArrayBasedStoreRBallPBMove extends RBallPBMove implements ArrayBasedMove<RBallPBMove> {
    private int index;
    
    public ArrayBasedStoreRBallPBMove(double improvement, SetOfVars variables) {
        super(improvement, variables);
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
    public RBallPBMove getMove() {
        return this;
    }

}
