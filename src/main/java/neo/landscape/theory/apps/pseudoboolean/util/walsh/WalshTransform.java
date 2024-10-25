package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

import java.util.HashSet;
import java.util.Set;

public class WalshTransform {

    public static WalshCoefficients transform(EmbeddedLandscape el) {
        WalshCoefficients result = new WalshCoefficients();
        for (int sf=0; sf < el.getM(); sf++) {
            computeTransformForSubfunction(el, sf, result);
        }

        return result;
    }

    private static void computeTransformForSubfunction(EmbeddedLandscape el, int sf, WalshCoefficients result) {
        int k = el.getMaskLength(sf);
        if (k > 29) {
            throw new IllegalArgumentException(String.format("The mask length (%1$d) is too big for the Walsh transform", k));
        }
        long maxValue = 1 << k;
        for (int w=0; w < maxValue; w++) {
            Set<Integer> index = getIndex(el, sf, k, w);
            double value = 0.0;
            for (int x=0; x < maxValue; x++) {
                double sfEval = el.evaluateSubfunction(sf, x);
                if (walsh(x, w) > 0) {
                    value += sfEval;
                } else {
                    value -= sfEval;
                }
            }
            value /= maxValue;
            result.addCoefficient(index, value);
        }
    }

    private static Set<Integer> getIndex(EmbeddedLandscape el, int sf, int k, int w) {
        Set<Integer> index = new HashSet<>();
        for (int i = 0; i < k; i++) {
            if ((w & 0x01) == 1){
                index.add(el.getMasks(sf, i));
            }
            w >>>= 1;
        }
        return index;
    }

    private static int walsh(int x, int w) {
        return ((Integer.bitCount(x & w) & 0x01) == 0)? 1: -1;
    }


}
