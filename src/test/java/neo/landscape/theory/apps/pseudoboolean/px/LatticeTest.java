package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import org.checkerframework.checker.units.qual.min;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Lattice Test")
public class LatticeTest {

    @ParameterizedTest
    @MethodSource("provideParamsForTests")
    public void testContainsWhenItContains(int n, long seed) {
        Random rnd = new Random (seed);
        PBSolution baseSolution = randomSolution(n, rnd);
        List<Integer> vars = IntStream.range(0, n)
                                .boxed()
                                .collect(Collectors.toList());
        Collections.shuffle(vars, rnd);

        List<List<Integer>> components = getComponents(1, 7, vars, rnd);
        Lattice latticeParent = new Lattice(baseSolution, components);
        Lattice lattice = new Lattice(baseSolution, components.subList(0, rnd.nextInt(components.size())));

        assertThat(latticeParent.contains(lattice)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideParamsForTests")
    public void testContainsWhenItDoesNotContainTrivial(int n, long seed) {
        Random rnd = new Random (seed);
        PBSolution baseSolution = randomSolution(n, rnd);
        List<Integer> vars = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(vars, rnd);

        List<List<Integer>> components = getComponents(2, 7, vars, rnd);
        Lattice latticeParent = new Lattice(baseSolution, components);
        Lattice lattice = new Lattice(baseSolution, components.subList(0, rnd.nextInt(components.size()-1)));

        assertThat(lattice.contains(latticeParent)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideParamsForTests")
    public void testContainsWhenItContainsWithDifferentBaseSolution(int n, long seed) {
        Random rnd = new Random (seed);
        PBSolution baseSolution = randomSolution(n, rnd);
        List<Integer> vars = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(vars, rnd);

        List<List<Integer>> components = getComponents(1, 7, vars, rnd);
        Lattice latticeParent = new Lattice(baseSolution, components);

        List<List<Integer>> componentsForChild = components.subList(0, rnd.nextInt(components.size()));
        PBSolution childBaseSolution = buildChildSolution(baseSolution, rnd, componentsForChild);
        Lattice lattice = new Lattice(childBaseSolution, componentsForChild);

        assertThat(latticeParent.contains(lattice)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideParamsForTests")
    public void testContainsWhenItDoesNotContainFlippingABit(int n, long seed) {
        Random rnd = new Random (seed);
        PBSolution baseSolution = randomSolution(n, rnd);
        List<Integer> vars = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(vars, rnd);

        List<List<Integer>> components = getComponents(2, 7, vars, rnd);
        Lattice latticeParent = new Lattice(baseSolution, components);

        List<Integer> varsToFlip = components.stream()
            .filter(c->c.size()> 1)
            .flatMap(c->c.stream())
            .collect(Collectors.toList());

        if (!varsToFlip.isEmpty()) {
            int var = varsToFlip.get(rnd.nextInt(varsToFlip.size()));
            PBSolution childSolution = new PBSolution(baseSolution);
            childSolution.flipBit(var);

            Lattice lattice = new Lattice(childSolution, components);
            assertThat(latticeParent.contains(lattice)).isFalse();
        }
    }

    @ParameterizedTest
    @MethodSource("provideParamsForTests")
    public void testAgreementWithExhaustiveAnalysis(int n, long seed) {
        Random rnd = new Random(seed);
        Lattice parentLattice = generateLattice(n, rnd.nextLong());
        Lattice childLattice = generateLattice(n, rnd.nextLong());

        assertThat(parentLattice.contains(childLattice)).isEqualTo(contains(parentLattice, childLattice));
    }

    private boolean contains(Lattice parent, Lattice child) {
        List<PBSolution> parentSolutions = parent.computeAllSolutions().collect(Collectors.toList());
        List<PBSolution> childSolutions = child.computeAllSolutions().collect(Collectors.toList());

        return childSolutions.stream()
            .allMatch(childSolution -> parentSolutions.contains(childSolution));
    }

    private Lattice generateLattice(int n, long seed) {
        Random rnd = new Random (seed);
        PBSolution baseSolution = randomSolution(n, rnd);
        List<Integer> vars = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(vars, rnd);

        List<List<Integer>> components = getComponents(2, 7, vars, rnd);
        return new Lattice(baseSolution, components);
    }

    private static PBSolution buildChildSolution(PBSolution baseSolution, Random rnd, List<List<Integer>> componentsForChild) {
        PBSolution childBaseSolution = new PBSolution(baseSolution);
        int indexedSoltuion =  rnd.nextInt(1 << componentsForChild.size());
        for (int c = 0; c < componentsForChild.size(); c++) {
            if ((indexedSoltuion & (1 << c)) != 0) {
                for (int var : componentsForChild.get(c)) {
                    childBaseSolution.flipBit(var);
                }
            }
        }
        return childBaseSolution;
    }

    private static Stream<Arguments> provideParamsForTests() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int N : IntStream.of(4, 5, 6, 7, 8, 9, 10, 50, 100, 500, 1000).toArray()) {
            for (long seed : IntStream.rangeClosed(1, 10).toArray()) {
                builder.add(arguments(N, seed));
            }
        }
        return builder.build();
    }

    private List<List<Integer>> getComponents(int min, int max, List<Integer> vars, Random rnd) {
        int numberOfComponents = Math.min(vars.size(), rnd.nextInt(max-min+1) + min);

        int startVar = 0;
        List<List<Integer>> components = new ArrayList<>();
        for (int component = 0; component < numberOfComponents; component++) {
            int remainingComponents = numberOfComponents-component;
            int endOfComponent = rnd.nextInt(vars.size()-startVar-remainingComponents+1)+startVar;
            components.add(vars.subList(startVar, endOfComponent+1));
            startVar = endOfComponent+1;
        }
        return components;
    }

    private PBSolution randomSolution(int n, Random rnd) {
        PBSolution solution = new PBSolution (n);
        IntStream.range(0, n)
            .forEach(i -> solution.setBit(i, rnd.nextInt(2)));
        return solution;
    }


}
