package neo.landscape.theory.apps.pseudoboolean.util.walsh;

public interface WalshCoefficientsFactory<W extends WalshCoefficientsInterface> {
    W create(int n);
}
