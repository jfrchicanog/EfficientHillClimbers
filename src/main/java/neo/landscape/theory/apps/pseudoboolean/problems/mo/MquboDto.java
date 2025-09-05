package neo.landscape.theory.apps.pseudoboolean.problems.mo;

public class MquboDto {

    public static class WalshCoefficientDto {
        public double w;
        public int [] ids;
    }

    public static class Problem {
        public String type;
        public int n;
        public long seed;
        public int m;
        public WalshCoefficientDto terms [][];
    }

    public Problem problem;
}
