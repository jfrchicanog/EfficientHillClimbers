package neo.landscape.theory.apps.pseudoboolean.util.walsh;

import java.util.Set;

public interface WalshCoefficientsInterface {
    void addCoefficient(Set<Integer> variables, double value);
    int getNonzeroTerms();
    double getCoefficient(Set<Integer> variables);
}
