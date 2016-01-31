package neo.landscape.theory.apps.pseudoboolean.util.movestore;

import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;


public interface ArrayBasedMoveFactory<M> {
    public ArrayBasedMove<M> createArrayBasedMove(SetOfVars sov);
    public SetOfVars getSetOfVars(ArrayBasedMove<M> move);

}
