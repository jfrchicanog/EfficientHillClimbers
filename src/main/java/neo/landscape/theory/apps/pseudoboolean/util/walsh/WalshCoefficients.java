package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import java.util.*;
import java.util.stream.Stream;

public class WalshCoefficients implements Iterable<WalshCoefficient>, WalshCoefficientsInterface {
    private Map<Set<Integer>, WalshCoefficient> coefficients = new HashMap<>();
    private Map<Integer, List<WalshCoefficient>> wcByVariable = new HashMap<>();

    public double getCoefficient(Set<Integer> index) {
        return coefficients.getOrDefault(index, new WalshCoefficient(index)).value;
    }

    public void setCoefficient(WalshCoefficient coefficient) {
        if (coefficient.value == 0) {
            removeCoefficientForVariables(coefficient.variables);
        } else {
            replaceCoefficientForVariables(coefficient);
        }
    }

    private void removeCoefficientForVariables(Set<Integer> variables) {
        WalshCoefficient coeff = coefficients.remove(variables);
        if (coeff != null) {
            removeFromWcByVar(coeff);
        }
    }

    private void removeFromWcByVar(WalshCoefficient coeff) {
        coeff.variables.forEach(var -> {
            wcByVariable.computeIfPresent(var, (v, l) -> {
                l.remove(coeff);
                return l;
            });
        });
    }

    public List<WalshCoefficient> getCoefficientsForVariable(int var) {
        return wcByVariable.getOrDefault(var, Collections.emptyList());
    }

    private void replaceCoefficientForVariables(WalshCoefficient coefficient) {
        WalshCoefficient coeff = coefficients.put(coefficient.variables, coefficient);
        replaceInWcByVar(coefficient, coeff);
    }

    private void replaceInWcByVar(WalshCoefficient coefficient, WalshCoefficient coeff) {
        coefficient.variables.forEach(var -> {
            wcByVariable.compute(var, (v, l) -> {
                if (l == null) {
                    l = new ArrayList<>();
                }
                if (coeff != null) {
                    l.remove(coeff);
                }
                l.add(coefficient);
                return l;
            });
        });
    }

    public void addCoefficient(WalshCoefficient coefficient) {
        if (coefficient.value == 0) {
            return;
        }
        coefficients.compute(coefficient.variables, (k, v) -> {
            if (v == null) {
                replaceInWcByVar(coefficient, null);
                return coefficient;
            } else {
                v.value += coefficient.value;
                if (v.value == 0) {
                    removeFromWcByVar(v);
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
        addCoefficient(new WalshCoefficient(vars, value));
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

    @Override
    public Iterator<WalshCoefficient> iterator() {
        return coefficients.values().iterator();
    }

    public static WalshCoefficientsFactory<WalshCoefficients> factory() {
        return (int n) -> new WalshCoefficients();
    }
}
