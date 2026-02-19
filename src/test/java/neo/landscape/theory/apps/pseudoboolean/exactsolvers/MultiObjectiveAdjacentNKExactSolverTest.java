package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.problems.mo.MNKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiObjectiveAdjacentNKExactSolverTest {

    @ParameterizedTest
    @MethodSource("argumentsForNK")
    public void compareExhaustiveAndWright(int n, int k, int pseed) {
        VectorMKLandscape problem = getVectorMKLandscape(n, k, pseed);

        MultiObjectiveAdjacentNKExactSolver<ParetoNonDominatedSet2D> solver = new MultiObjectiveAdjacentNKExactSolver<>(new ParetoNonDominatedSet2DFactory());
        ParetoNonDominatedSet2D actual = solver.computeParetoFront(problem);

        MultiObjectiveCompleteEnumeration exhaustive = new MultiObjectiveCompleteEnumeration();
        ParetoNonDominatedSet expected = exhaustive.solve(problem);

        compareSets(actual, expected);

    }

    private static VectorMKLandscape getVectorMKLandscape(int n, int k, int pseed) {
        Properties properties = new Properties();
        properties.setProperty(MNKLandscapeConfigurator.N_ARGUMENT, String.valueOf(n));
        properties.setProperty(MNKLandscapeConfigurator.K_ARGUMENT, String.valueOf(k));
        properties.setProperty(MNKLandscapeConfigurator.D_ARGUMENT, String.valueOf(2));
        properties.setProperty(MNKLandscapeConfigurator.MODEL_ARGUMENT, "adjacent");
        properties.setProperty(MNKLandscapeConfigurator.Q_ARGUMENT, "100");
        properties.setProperty(MNKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, String.valueOf(pseed));

        MNKLandscapeConfigurator configurator = new MNKLandscapeConfigurator();
        VectorMKLandscape problem = configurator.configureProblem(properties, null);
        return problem;
    }

    private void compareSets(ParetoNonDominatedSet2D actual, ParetoNonDominatedSet expected) {
        assertEquals(actual.size(), expected.size(), "Sizes differ between the two sets");
        // Compare contents
        List<double[] > generalList = expected.stream().sorted(ParetoNonDominatedSet2D.COMPARATOR_2D).toList();
        List <double[] > twoDList = actual.stream().toList();
        compareLists(generalList, twoDList);

    }

    private static void compareLists(List<double[]> s1list, List<double[]> s2list) {
        for (int i = 0; i < s1list.size(); i++) {
            double[] p1 = s1list.get(i);
            double[] p2 = s2list.get(i);
            assertArrayEquals(p1, p2, "The point in position "+i+" differs between the actual and expteted set");
        }
    }

    private static Stream<Arguments> argumentsForNK() {
        return IntStream.rangeClosed(1,4).boxed()
                .flatMap(n -> IntStream.rangeClosed(0,4).boxed()
                .flatMap(k->IntStream.rangeClosed(0,9).boxed()
                    .map(pseed->Arguments.of(5*n,k,pseed))));
    }

    private static Stream<Arguments> argumentsForPerformance() {
        return IntStream.rangeClosed(10,10).boxed()
            .flatMap(n -> IntStream.rangeClosed(2,2).boxed()
                .flatMap(k->IntStream.rangeClosed(0,0).boxed()
                    .map(pseed->Arguments.of(100*n,k,pseed))));
    }

    @ParameterizedTest
    @MethodSource("argumentsForPerformance")
    public void performanceTest(int n, int k, int pseed) {
        VectorMKLandscape problem = getVectorMKLandscape(n, k, pseed);

        MultiObjectiveAdjacentNKExactSolver<?> solver = new MultiObjectiveAdjacentNKExactSolver<>(new ParetoNonDominatedSet2DEfficientFactory());

        long start = System.nanoTime();
        IParetoNonDominatedSet<?> actual = solver.computeParetoFront(problem);
        long efficientTime = System.nanoTime() - start;
        System.out.println("Effective time: " + efficientTime + " nanoseconds");
    }

    @ParameterizedTest
    @MethodSource("argumentsForPerformance")
    public void performanceTestGenericNSSet(int n, int k, int pseed) {
        VectorMKLandscape problem = getVectorMKLandscape(n, k, pseed);

        MultiObjectiveAdjacentNKExactSolver<ParetoNonDominatedSet> solver = new MultiObjectiveAdjacentNKExactSolver<>(new ParetoNonDominatedSetFactory());

        long start = System.nanoTime();
        ParetoNonDominatedSet actual = solver.computeParetoFront(problem);
        long efficientTime = System.nanoTime() - start;
        System.out.println("Effective time: " + efficientTime + " nanoseconds");
    }
}
