package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import java.util.HashSet;
import java.util.Set;

public class WalshCoefficient {
    public double value;
    public Set<Integer> variables;

    public WalshCoefficient(Set<Integer> vars, double val) {
        this.variables = new HashSet<>(vars);
        this.value = val;
    }

    public WalshCoefficient(Set<Integer> vars) {
        this(vars, 0);
    }
}
