package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WalshConstraint {



    public static WalshCoefficient constraint(WalshCoefficient wc, PBSolution red, PBSolution xor) {
        double sign = 1.0;
        //Set<Integer> newVars = new HashSet<>();
        int [] newVarsArray = new int[wc.variables.size()];
        int newVarsIndex = 0;
        for (int variable : wc.variables) {
            if (xor.getBit(variable) == 1) {
                //newVars.add(variable);
                newVarsArray[newVarsIndex++] = variable;
            } else if (red.getBit(variable) == 1) {
                sign = -sign;
            }
        }

        Set<Integer> newVars = Arrays.stream(newVarsArray, 0, newVarsIndex)
            .boxed()
            .collect(Collectors.toSet());
        return new WalshCoefficient(newVars, sign * wc.value);
    }

    public static WalshCoefficients constraint(WalshCoefficients wc, PBSolution red, PBSolution blue) {
        PBSolution xor = red.xor(blue);

        WalshCoefficients newWC = new WalshCoefficients(); // FIXME: reduce the time to compute this
        Set<WalshCoefficient> explored = new HashSet<>();

        for (int var=0; var < red.getN(); var++) {
            if (xor.getBit(var) == 1) {
                List<WalshCoefficient> wcs = wc.getCoefficientsForVariable(var);
                if (wcs != null) {
                    for (WalshCoefficient wc1: wcs) {
                        if (!explored.contains(wc1)) {
                            WalshCoefficient ww = constraint(wc1, red, xor);
                            newWC.addCoefficient(ww);
                            explored.add(wc1);
                        }

                    }
                }
            }
        }
/*
        for (WalshCoefficient wc1: wc) {
            WalshCoefficient ww =constraint(wc1, red, xor);
            newWC.addCoefficient(ww);
        }*/
        /*
        wc.stream()
            .map(wc1 -> constraint(wc1, red, xor))
            .forEach(newWC::addCoefficient);*/
        return newWC;
    }
}
