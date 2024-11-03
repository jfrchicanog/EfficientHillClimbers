package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.HashSet;
import java.util.Set;

public class WalshCoefficient {
    public double value;
    public Set<Integer> variables;
    public int id;

    public WalshCoefficient(Set<Integer> vars, double val, int id) {
        this.variables = vars;
        this.value = val;
        this.id = id;
    }

    public WalshCoefficient(Set<Integer> vars) {
        this(vars, 0, 0);
    }

    public double evaluate(PBSolution solution) {
        int oneBits = 0;
        for (int var: variables) {
            oneBits += solution.getBit(var);
        }
        return ((oneBits&1)==0)?value:-value;
    }
}
