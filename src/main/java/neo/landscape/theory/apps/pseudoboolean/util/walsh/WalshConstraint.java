package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WalshConstraint {

    private static WalshCoefficient constraint(WalshCoefficientsInterface wc, int id, PBSolution red, PBSolution xor) {
        final AtomicInteger sign = new AtomicInteger(0);
        Set<Integer> newVars = new HashSet<>();
        wc.getVarsForID(id).forEach(var -> {
            if (xor.getBit(var) == 1) {
                newVars.add(var);
            } else if (red.getBit(var) == 1) {
                sign.incrementAndGet();
            }
        });
        double value = wc.getCoefficient(id);
        return new WalshCoefficient(newVars, (sign.intValue()&1)==0?value:-value, 0);
    }

    public static <W extends WalshCoefficientsInterface<W>> W constraint(W wc, PBSolution red, PBSolution blue) {
        PBSolution xor = red.xor(blue);
        W newWC = wc.getFactory().create(red.getN());
        efficientImplementation(wc, red, xor, newWC);
        //originalImplementation(wc, red, xor, newWC);
        return newWC;
    }

    private static <W extends WalshCoefficientsInterface<W>> void efficientImplementation(W wc, PBSolution red, PBSolution xor, W newWC) {
        TwoStatesIntegerSet explored = new TwoStatesISArrayImpl(wc.getNumberOfIDs());

        for (int var = 0; var < red.getN(); var++) {
            if (xor.getBit(var) == 1) {
                IntStream wcs = wc.getCoefficientsForVariable(var);
                wcs.forEach(wc1 -> {
                    if (!explored.isExplored(wc1)) {
                        WalshCoefficient ww = constraint(wc, wc1, red, xor);
                        newWC.addCoefficient(ww.variables, ww.value);
                        explored.explored(wc1);
                    }
                });
            }
        }
    }

    private static <W extends WalshCoefficientsInterface<W>> void originalImplementation(W wc, PBSolution red, PBSolution xor, W newWC) {
        wc.getNonZeroCoefficients().forEach(wcID-> {
            WalshCoefficient ww = constraint(wc, wcID, red, xor);
            newWC.addCoefficient(ww.variables, ww.value);
        });
    }
}
