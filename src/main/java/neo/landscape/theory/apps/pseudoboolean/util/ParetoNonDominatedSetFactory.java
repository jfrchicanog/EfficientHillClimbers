package neo.landscape.theory.apps.pseudoboolean.util;

public class ParetoNonDominatedSetFactory implements IParetoNonDominatedSetFactory<ParetoNonDominatedSet>{
    @Override
    public ParetoNonDominatedSet create() {
        return new ParetoNonDominatedSet();
    }

    @Override
    public void combine(ParetoNonDominatedSet source1, double[] offset1, ParetoNonDominatedSet source2, double[] offset2, ParetoNonDominatedSet target) {
        target.clear();
        source1.stream().map(point -> addPoint(offset1, point)).forEach(target::addPoint);
        source2.stream().map(point -> addPoint(offset2, point)).forEach(target::addPoint);
    }

    private double [] addPoint(double [] point1, double [] point2) {
        double [] result = new double[point1.length];
        for (int i = 0; i < point1.length; i++) {
            result[i] = point1[i] + point2[i];
        }
        return result;
    }
}
