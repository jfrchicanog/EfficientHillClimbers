package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.util.Set;
import java.util.stream.IntStream;

public interface WalshCoefficientsInterface<W extends WalshCoefficientsInterface> {
    void addCoefficient(Set<Integer> variables, double value);
    int getNonzeroTerms();
    int getNumberOfIDs();
    double getCoefficient(int id);
    double getCoefficient(Set<Integer> variables);
    int numberOfVarsForID(int id);
    IntStream getVarsForID(int id);
    IntStream getCoefficientsForVariable(int var);
    IntStream getNonZeroCoefficients();
    double evaluate(int id, PBSolution solution);
    WalshCoefficientsFactory<W> getFactory();
}
