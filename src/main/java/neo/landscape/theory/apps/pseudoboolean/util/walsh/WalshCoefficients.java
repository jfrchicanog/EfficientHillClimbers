package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WalshCoefficients implements Iterable<WalshCoefficient>, WalshCoefficientsInterface<WalshCoefficients> {
    private static final WalshCoefficientsFactory<WalshCoefficients> walshCoefficientsWalshCoefficientsFactory = (int n) -> new WalshCoefficients();
    private Map<Set<Integer>, WalshCoefficient> coefficients = new HashMap<>();
    private Map<Integer, List<WalshCoefficient>> wcByVariable = new HashMap<>();
    private Map<Integer, WalshCoefficient> wcByID = new HashMap<>();
    private int nextID = 0;

    public double getCoefficient(Set<Integer> index) {
        return coefficients.getOrDefault(index, new WalshCoefficient(index)).value;
    }

    @Override
    public int numberOfVarsForID(int id) {
        return wcByID.getOrDefault(id, new WalshCoefficient(Collections.emptySet())).variables.size();
    }

    @Override
    public IntStream getVarsForID(int id) {
        return wcByID.getOrDefault(id, new WalshCoefficient(Collections.emptySet()))
            .variables
            .stream()
            .mapToInt(Integer::intValue);
    }

    private void removeFromWcByVar(WalshCoefficient coeff) {
        coeff.variables.forEach(var -> {
            wcByVariable.computeIfPresent(var, (v, l) -> {
                l.remove(coeff);
                return l;
            });
        });
    }

    public IntStream getCoefficientsForVariable(int var) {
        return wcByVariable.getOrDefault(var, Collections.emptyList())
            .stream()
            .mapToInt(wc -> wc.id);
    }

    @Override
    public IntStream getNonZeroCoefficients() {
        return coefficients.values().stream().mapToInt(wc -> wc.id);
    }

    @Override
    public double evaluate(int id, PBSolution solution) {
        return wcByID.getOrDefault(id, new WalshCoefficient(Collections.emptySet()))
            .evaluate(solution);
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
                coefficient.id = nextID++;
                wcByID.put(coefficient.id, coefficient);
                replaceInWcByVar(coefficient, null);
                return coefficient;
            } else {
                v.value += coefficient.value;
                if (v.value == 0) {
                    removeFromWcByVar(v);
                    wcByID.remove(v.id);
                    return null;
                }
                return v;
            }
        });
    }

    public int getNonzeroTerms() {
        return coefficients.size();
    }

    @Override
    public int getNumberOfIDs() {
        return nextID;
    }

    @Override
    public double getCoefficient(int id) {
        return wcByID.getOrDefault(id, new WalshCoefficient(Collections.emptySet())).value;
    }

    public void addCoefficient(Set<Integer> vars, double value) {
        addCoefficient(new WalshCoefficient(vars, value, 0));
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
        return walshCoefficientsWalshCoefficientsFactory;
    }

    public WalshCoefficientsFactory<WalshCoefficients> getFactory() {
        return WalshCoefficients.factory();
    }
}
