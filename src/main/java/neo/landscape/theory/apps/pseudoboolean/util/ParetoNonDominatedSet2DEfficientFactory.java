package neo.landscape.theory.apps.pseudoboolean.util;

public class ParetoNonDominatedSet2DEfficientFactory implements IParetoNonDominatedSetFactory<ParetoNonDominatedSet2DEfficient>{
    @Override
    public ParetoNonDominatedSet2DEfficient create() {
        return new ParetoNonDominatedSet2DEfficient();
    }

    @Override
    public void combine(ParetoNonDominatedSet2DEfficient source1, double[] offset1, ParetoNonDominatedSet2DEfficient source2, double[] offset2, ParetoNonDominatedSet2DEfficient target) {
        ParetoNonDominatedSet2DEfficient.combine(source1, offset1, source2, offset2, target);
    }
}
