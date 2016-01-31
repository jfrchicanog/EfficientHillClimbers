package neo.landscape.theory.apps.pseudoboolean.util.movestore;

public interface ArrayBasedMove<M> {
    public int getIndex();
    public void setIndex(int index);
    public M getMove();

}