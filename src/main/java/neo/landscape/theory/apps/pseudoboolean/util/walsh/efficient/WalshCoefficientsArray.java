package neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WalshCoefficientsArray implements neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface<WalshCoefficientsArray> {
    private static WalshCoefficientsFactory<WalshCoefficientsArray> walshCoeffFactory = (int n) -> new WalshCoefficientsArray(n);
    private int n;
    private int nonZeroWalshCoefficients;

    private int nextID;
    private double [] coefficients;
    private int [] variableSetSize;
    private int [] variableSetIndex;

    private int nextVariableIndex;
    private int [] variables;

    private Map<Set<Integer>, Integer> varSetToWalshCoefficientID;
    private Map<Set<Integer>, Integer> varSetToVariableIndex;
    private int [][] walshCoefficientsByVariable;

    public WalshCoefficientsArray(int n) {
        this.n = n;
        this.nonZeroWalshCoefficients = 0;
        this.nextID = 0;
        this.coefficients = new double[1];
        this.variableSetSize = new int[1];
        this.variableSetIndex = new int[1];
        this.nextVariableIndex = 0;
        this.variables = new int[1];
        this.varSetToWalshCoefficientID = new HashMap<>();
        this.varSetToVariableIndex = new HashMap<>();
        this.walshCoefficientsByVariable = null;
    }

    @Override
    public void addCoefficient(Set<Integer> variables, double value) {
        if (value == 0) {
            return;
        }
        Integer id = varSetToWalshCoefficientID.get(variables);
        if (id != null) {
            coefficients[id] += value;
            if (coefficients[id] == 0) {
                nonZeroWalshCoefficients--;
                removeCoefficient(id);
            }
        } else {
            // We add a new coefficient
            if (nextID == coefficients.length) {
                coefficients = Arrays.copyOf(coefficients, nextID * 2);
                variableSetIndex = Arrays.copyOf(variableSetIndex, nextID * 2);
                variableSetSize = Arrays.copyOf(variableSetSize, nextID * 2);
            }
            coefficients[nextID] = value;
            variableSetSize[nextID] = variables.size();
            varSetToWalshCoefficientID.put(variables, nextID);
            Integer varIndex = varSetToVariableIndex.get(variables);
            if (varIndex != null) {
                variableSetIndex[nextID] = varIndex;
            } else {
                if (nextVariableIndex + variables.size() > this.variables.length) {
                    this.variables = Arrays.copyOf(this.variables, Math.max(nextVariableIndex * 2, nextVariableIndex + variables.size()));
                }
                variableSetIndex[nextID] = nextVariableIndex;
                for (int var : variables) {
                    this.variables[nextVariableIndex++] = var;
                }
                varSetToVariableIndex.put(variables, variableSetIndex[nextID]);
            }
            nonZeroWalshCoefficients++;
            nextID++;
        }
    }

    private void removeCoefficient(Integer id) {
        Set<Integer> vars = Arrays.stream(variables, variableSetIndex[id], variableSetIndex[id] + variableSetSize[id])
            .boxed()
            .collect(Collectors.toSet());
        varSetToWalshCoefficientID.remove(vars);
        if (id == nextID-1) {
            nextID--;
            if (variableSetIndex[id]+vars.size() == nextVariableIndex) {
                nextVariableIndex = variableSetIndex[id];
                varSetToVariableIndex.remove(vars);
            }
        }
        walshCoefficientsByVariable = null;
    }

    @Override
    public int getNonzeroTerms() {
        return nonZeroWalshCoefficients;
    }


    @Override
    public int getNumberOfIDs() {
        return nextID;
    }

    @Override
    public double getCoefficient(int id) {
        if (id >= nextID) {
            throw new IllegalArgumentException("ID " + id + " is greater than the largest index " + (nextID-1));
        }
        return coefficients[id];
    }

    public int getIDForVariables(Set<Integer> variables) {
        return varSetToWalshCoefficientID.get(variables);
    }

    @Override
    public double getCoefficient(Set<Integer> variables) {
        Integer id = varSetToWalshCoefficientID.get(variables);
        return id == null? 0: coefficients[id];
    }

    @Override
    public int numberOfVarsForID(int id) {
        if (id >= nextID) {
            throw new IllegalArgumentException("ID " + id + " is greater than the largest index " + (nextID-1));
        }
        return variableSetSize[id];
    }


    @Override
    public IntStream getVarsForID(int id) {
        return Arrays.stream(variables, variableSetIndex[id], variableSetIndex[id] + variableSetSize[id]);
    }

    @Override
    public IntStream getCoefficientsForVariable(int variable) {
        if (walshCoefficientsByVariable == null) {
            buildWalshCoefficientsByVariable();
        }
        return IntStream.of(walshCoefficientsByVariable[variable]);

    }

    @Override
    public IntStream getNonZeroCoefficients() {
        return IntStream.range(0, nextID)
            .filter(j->coefficients[j] !=0);
    }

    @Override
    public double evaluate(int id, PBSolution solution) {
        if (id >= nextID) {
            throw new IllegalArgumentException("ID " + id + " is greater than the largest index " + (nextID-1));
        }

        if (coefficients[id] == 0) {
            return 0;
        }

        int oneBits = 0;
        for (int index=variableSetIndex[id]; index < variableSetIndex[id] + variableSetSize[id]; index++) {
            oneBits += solution.getBit(variables[index]);
        }
        return ((oneBits&1)==0)?coefficients[id]:-coefficients[id];
    }

    private void buildWalshCoefficientsByVariable() {
        List<Integer> auxiliaryList [] = new List[n];
        for (int i = 0; i < n; i++) {
            auxiliaryList[i] = new ArrayList<>();
        }

        getNonZeroCoefficients()
            .forEach(c->{
                for (int i = variableSetIndex[c]; i < variableSetIndex[c] + variableSetSize[c]; i++) {
                    auxiliaryList[variables[i]].add(c);
                }
            });

        walshCoefficientsByVariable = new int[n][];
        for (int i = 0; i < n; i++) {
            walshCoefficientsByVariable[i] = auxiliaryList[i].stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public static WalshCoefficientsFactory<WalshCoefficientsArray> factory() {
        return walshCoeffFactory;
    }

    public WalshCoefficientsFactory<WalshCoefficientsArray> getFactory() {
        return WalshCoefficientsArray.factory();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        getNonZeroCoefficients().forEach(id -> {
            Set<Integer> vars = getVarsForID(id).boxed().collect(Collectors.toSet());
            sb.append(vars).append(" -> ").append(coefficients[id]).append("\n");
        });

        return sb.toString();
    }

}
