package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.HashSet;
import java.util.Set;

public class WalshConstraint {
    public static WalshCoefficient constraint(WalshCoefficient wc, PBSolution red, PBSolution blue) {
        double sign = 1.0;
        Set<Integer> newVars = new HashSet<>();
        for (int variable : wc.variables) {
            if (red.getBit(variable) != blue.getBit(variable)) {
                newVars.add(variable);
            } else if (red.getBit(variable) == 1) {
                sign = -sign;
            }
        }
        return new WalshCoefficient(newVars, sign * wc.value);
    }

    public static WalshCoefficients constraint(WalshCoefficients wc, PBSolution red, PBSolution blue) {
        WalshCoefficients newWC = new WalshCoefficients();
        wc.stream().map(wc1 -> constraint(wc1, red, blue)).forEach(newWC::addCoefficient);
        return newWC;
    }
}
