package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.experiments.loma.LocalOptimaNetworkGoldman;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.MNKLandscapeConfigurator;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet2D;
import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet2DFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Tag;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CPBasedHPEliminationTest {

    @ParameterizedTest
    @MethodSource("argumentsForNK")
    @Tag("cpbased")
    public void compareCPWithGoldman(int n, int k, int pseed) {
        EmbeddedLandscape problem = getNKLandscape(n, k, pseed);

        CPBasedHyperplaneElimination solver = new CPBasedHyperplaneElimination(problem);

        long startTime = System.nanoTime();
        Set<PBSolution> localOptima = solver.findParetoLocalOptima();
        long cpTime = System.nanoTime() - startTime;
        startTime = System.nanoTime();
        Set<PBSolution> goldmanLocalOptima = findLocalOptimaGoldman(problem);
        long goldmanTime = System.nanoTime() - startTime;

        assertEquals(goldmanLocalOptima.size(), localOptima.size(), "Sizes differ between the two sets");
        // Compare contents
        assertEquals(localOptima, goldmanLocalOptima, "The sets of local optima differ");
        if (goldmanTime < cpTime) {
            System.out.println("Goldman faster in "+n+","+k+","+pseed+" by "+(cpTime-goldmanTime)/1e3+" µs");
        }

    }

    @ParameterizedTest
    @MethodSource("argumentsForNK")
    @Tag("cpbased")
    public void performanceTest(int n, int k, int pseed) {
        EmbeddedLandscape problem = getNKLandscape(n, k, pseed);

        CPBasedHyperplaneElimination solver = new CPBasedHyperplaneElimination(problem);

        long startTime = System.nanoTime();
        Set<PBSolution> localOptima = solver.findParetoLocalOptima();
        long cpTime = System.nanoTime() - startTime;
        startTime = System.nanoTime();
        Set<PBSolution> goldmanLocalOptima = findLocalOptimaGoldman(problem);
        long goldmanTime = System.nanoTime() - startTime;

        assertThat(goldmanTime).isGreaterThanOrEqualTo(cpTime);

    }

    private static Set<PBSolution> findLocalOptimaGoldman(EmbeddedLandscape pbf) {
        LocalOptimaNetworkGoldman goldman = new LocalOptimaNetworkGoldman();
        goldman.r = 1;
        goldman.seed = 0L;
        goldman.setPbf(pbf);
        goldman.prepareRBallExplorationAlgorithm();
        goldman.findLocalOptima();
        return new HashSet<>(goldman.localOptima);
    }

    private static EmbeddedLandscape getNKLandscape(int n, int k, int pseed) {
        Properties properties = new Properties();
        properties.setProperty(MNKLandscapeConfigurator.N_ARGUMENT, String.valueOf(n));
        properties.setProperty(MNKLandscapeConfigurator.K_ARGUMENT, String.valueOf(k));
        properties.setProperty(MNKLandscapeConfigurator.Q_ARGUMENT, "100");
        properties.setProperty(MNKLandscapeConfigurator.MODEL_ARGUMENT, "adjacent");
        properties.setProperty(MNKLandscapeConfigurator.PROBLEM_SEED_ARGUMENT, String.valueOf(pseed));

        EmbeddedLandscapeConfigurator configurator = new NKLandscapeConfigurator();
        return configurator.configureProblem(properties, null);
    }

    private static Stream<Arguments> argumentsForNK() {
        return IntStream.rangeClosed(1,4).boxed()
            .flatMap(n -> IntStream.rangeClosed(0,4).boxed()
                .flatMap(k->IntStream.rangeClosed(0,9).boxed()
                    .map(pseed->Arguments.of(5*n,k,pseed))));
    }

}
