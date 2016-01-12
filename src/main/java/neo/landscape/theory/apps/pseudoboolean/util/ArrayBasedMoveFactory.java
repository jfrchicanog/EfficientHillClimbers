package neo.landscape.theory.apps.pseudoboolean.util;


public interface ArrayBasedMoveFactory<M> {
    public ArrayBasedMove<M> createArrayBasedMove(SetOfVars sov);
    public SetOfVars getSetOfVars(ArrayBasedMove<M> move);

}
