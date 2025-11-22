package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.stream.Stream;

public interface IParetoNonDominatedSet<NS extends IParetoNonDominatedSet<NS>> {
    void addPoint(double [] point);
    Stream<double[]> stream();
    IParetoNonDominatedSetFactory<NS> getFactory();
    void clear();
    int size();
}
