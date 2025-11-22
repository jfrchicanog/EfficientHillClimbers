package neo.landscape.theory.apps.pseudoboolean.util;

public class ParetoNonDominatedSet2DFactory implements IParetoNonDominatedSetFactory<ParetoNonDominatedSet2D>{
    @Override
    public ParetoNonDominatedSet2D create() {
        return new ParetoNonDominatedSet2D();
    }

    @Override
    public void combine(ParetoNonDominatedSet2D source1, double[] offset1, ParetoNonDominatedSet2D source2, double[] offset2, ParetoNonDominatedSet2D target) {
        ParetoNonDominatedSet2D.combine(source1, offset1, source2, offset2, target);
    }
}
