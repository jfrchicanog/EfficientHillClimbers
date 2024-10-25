package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class WalshCoefficients {
    private Map<Set<Integer>, WalshCoefficient> coefficients = new HashMap<>();

    public WalshCoefficient getCoefficient(Set<Integer> index) {
        return coefficients.getOrDefault(index, new WalshCoefficient(index));
    }

    public void setCoefficient(WalshCoefficient coefficient) {
        if (coefficient.value == 0) {
            coefficients.remove(coefficient.variables);
        } else {
            coefficients.put(coefficient.variables, coefficient);
        }
    }

    public void addCoefficient(WalshCoefficient coefficient) {
        if (coefficient.value == 0) {
            return;
        }
        coefficients.compute(coefficient.variables, (k, v) -> {
            if (v == null) {
                return coefficient;
            } else {
                v.value += coefficient.value;
                if (v.value == 0) {
                    return null;
                }
                return v;
            }
        });
    }

    public int getNonzeroTerms() {
        return coefficients.size();
    }

    public void addCoefficient(Set<Integer> vars, double value) {
        if (value == 0) {
            return;
        }
        coefficients.compute(vars, (k, v) -> {
            if (v == null) {
                return new WalshCoefficient(vars, value);
            } else {
                v.value += value;
                if (v.value == 0) {
                    return null;
                }
                return v;
            }
        });
    }

    public void clear() {
        coefficients.clear();
    }

    public Stream<WalshCoefficient> stream() {
        return coefficients.values().stream();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (WalshCoefficient wc : coefficients.values()) {
            sb.append(wc.variables).append(" -> ").append(wc.value).append("\n");
        }
        return sb.toString();
    }
}
