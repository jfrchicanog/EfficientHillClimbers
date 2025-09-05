package neo.landscape.theory.apps.pseudoboolean.experiments.mqubos;
import java.math.BigInteger;

public class FubiniNumbers {

    // Función que calcula S(n,m) usando DP
    private static BigInteger stirlingSecond(int n, int m) {
        BigInteger[][] S = new BigInteger[n + 1][m + 1];

        // Inicializamos todos los valores a cero
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                S[i][j] = BigInteger.ZERO;
            }
        }

        S[0][0] = BigInteger.ONE; // base

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                // Recurrencia: S(n,m) = m*S(n-1,m) + S(n-1,m-1)
                S[i][j] = S[i - 1][j].multiply(BigInteger.valueOf(j))
                    .add(S[i - 1][j - 1]);
            }
        }

        return S[n][m];
    }

    public static BigInteger ontoNumber(int n, int m) {
        BigInteger factorial = factorial(m);
        BigInteger S = stirlingSecond(n, m);
        return factorial.multiply(S);
    }

    // Función que calcula F_n (número de Fubini)
    public static BigInteger fubini(int n) {
        BigInteger sum = BigInteger.ZERO;
        for (int m = 1; m <= n; m++) {
            sum = sum.add(ontoNumber(n,m));
        }
        return sum;
    }

    // Función para calcular factorial de manera eficiente con BigInteger
    private static BigInteger factorial(int n) {
        BigInteger f = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            f = f.multiply(BigInteger.valueOf(i));
        }
        return f;
    }

    // Ejemplo de uso
    public static void main(String[] args) {
        int n = 5;
        System.out.println("Fubini(" + n + ") = " + fubini(n));
    }
}