package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.Iterator;

public interface PartitionComponent extends Iterable<Integer>{

    public void clearComponent();
    public void addVarToComponent(int var);
    public Iterator<Integer> iterator();

}