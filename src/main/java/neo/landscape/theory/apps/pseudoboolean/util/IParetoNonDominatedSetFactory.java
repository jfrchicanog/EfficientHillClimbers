package neo.landscape.theory.apps.pseudoboolean.util;

public interface IParetoNonDominatedSetFactory<NS extends IParetoNonDominatedSet> {
    NS create();
    void combine(NS source1, double [] offset1, NS source2, double [] offset2, NS target);
}
