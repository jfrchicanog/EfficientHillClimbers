package neo.landscape.theory.apps.pseudoboolean.util.movestore.multicrieria;

public interface ArrayBasedMove<M> {
    public int getIndex(int criterion);
    public void setIndex(int criterion, int index);
    public M getMove();

}