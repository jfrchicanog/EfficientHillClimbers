package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.List;
import java.util.stream.Collectors;

public interface IParetoNonDominatedSetFactory<NS extends IParetoNonDominatedSet<NS>> {
    NS create();
    void combine(NS source1, double [] offset1, NS source2, double [] offset2, NS target);
    default void convolute(NS source1, NS source2, NS target) {
        var list1 = (List<double[]>) source1.stream().collect(Collectors.toList());
        var list2 = (List<double[]>) source2.stream().collect(Collectors.toList());
        target.clear();
        for (var point1 : list1) {
            for (var point2 : list2) {
                double [] newPoint = new double[point1.length];
                for (int i = 0; i < point1.length; i++) {
                    newPoint[i] = point1[i] + point2[i];
                }
                target.addPoint(newPoint);
            }
        }
    };
}
