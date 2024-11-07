package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficient;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class FourierPartitionCrossoverTest {

    private static Stream<Arguments> provideParamsForNKLandscpaes() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int N: IntStream.of(4, 5, 6, 7, 8, 9, 10, 50, 100, 500, 1000).toArray()) {
            for (int K: IntStream.range(1, Math.min(N-1, 5)).toArray()) {
                for (long seed: IntStream.rangeClosed(1,10).toArray()) {
                    builder.add(arguments(N, K, seed));
                }
            }
        }
        return builder.build();
    }


    @ParameterizedTest
    @MethodSource("provideParamsForNKLandscpaes")
    //@Disabled
    public void testFPX(int N, int K, long seed) {
        EmbeddedLandscape el = createNKLandscape(N, K, seed);
        WalshCoefficientsInterface wcs = WalshTransform.transform(el, WalshCoefficients.factory());
        WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
        FourierPartitionCrossover<WalshCoefficients> fpx = new FourierPartitionCrossover(wbf, el);
        fpx.setDebug(true);
        //fpx.setPrintStream(System.out);

        for (int i = 0; i < 10; i++) {
            PBSolution blue = el.getRandomSolution();
            PBSolution red = el.getRandomSolution();
            PBSolution solution = fpx.recombine(blue, red);
            checkVariablesInComponents(fpx);
            checkLattice(fpx, red);
        }
    }

    @ParameterizedTest
    @MethodSource("provideParamsForNKLandscpaes")
    //@Disabled
    public void testFPXArray(int N, int K, long seed) {
        EmbeddedLandscape el = createNKLandscape(N, K, seed);
        WalshCoefficientsInterface wcs = WalshTransform.transform(el, WalshCoefficientsArray.factory());
        WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
        FourierPartitionCrossover<WalshCoefficients> fpx = new FourierPartitionCrossover(wbf, el);
        fpx.setDebug(true);
        //fpx.setPrintStream(System.out);

        for (int i = 0; i < 10; i++) {
            PBSolution blue = el.getRandomSolution();
            PBSolution red = el.getRandomSolution();
            PBSolution solution = fpx.recombine(blue, red);
            checkVariablesInComponents(fpx);
            checkLattice(fpx, red);
        }
    }

    @Test
    public void testFPX847Array() {
        testFPXArray(8, 4, 7);
    }

    @Test
    public void testFPX847() {
        testFPX(8, 4, 7);
    }


    private NKLandscapes createNKLandscape(int N, int K, long seed) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, ""+K);
        prop.setProperty(NKLandscapes.Q_STRING, "64");
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "random");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }

    private void checkVariablesInComponents(FourierPartitionCrossover fpx) {
        List<Set<Integer>> varsInComponents = fpx.getVarsInComponents();

        fpx.getWcsConstrained().getNonZeroCoefficients()
            .forEach(wc->{
                Set<Integer> wcVariables = fpx.getWcsConstrained().getVarsForID(wc).boxed().collect(Collectors.toSet());
                long [] oddCounts = varsInComponents.stream().mapToLong(varSet ->
                        varSet.stream().filter(wcVariables::contains).count()
                    )
                    .filter(l-> (l&1)!=0).toArray();

                assertThat(oddCounts.length).isLessThanOrEqualTo(1)
                    .withFailMessage("There are more than one odd count in component. Walsh variables "
                    +wcVariables+".\n"+"Counts: "+ Arrays.toString(oddCounts));
            });
    }

    private void checkLattice(FourierPartitionCrossover fpx, PBSolution red) {
        List<Set<Integer>> varsInComponents = fpx.getVarsInComponents();
        List<Double> redValues = fpx.getRedValues();
        if (redValues.size() <= 10) {
            final int max = 1 << redValues.size();
            double redDifference = buildValueFromMask(redValues, 0);
            double baseFitness = fpx.getEmbddedLandscape().evaluate(red)-redDifference;
            for (int x=0; x < max; x++) {
                PBSolution child = buildSolutionFromMask(red, varsInComponents, x);
                double value = baseFitness + buildValueFromMask(redValues, x);
                double expected = fpx.getEmbddedLandscape().evaluate(child);
                assertThat(value).isEqualTo(expected, withPrecision(0.0001d))
                    .withFailMessage("The value does not match for "+child+ " where read parent is "+red);
            }
        }
    }

    private PBSolution buildSolutionFromMask(PBSolution red, List<Set<Integer>> varsInComponents, int mask) {
        PBSolution child = new PBSolution(red);
        for (int i=0; i < varsInComponents.size(); i++) {
            if ((mask & 1) != 0) {
                varsInComponents.get(i).forEach(child::flipBit);
            }
            mask >>>= 1;
        }
        return child;
    }

    private double buildValueFromMask(List<Double> redValues, int mask) {
        double result = 0;
        for (int i=0; i < redValues.size(); i++) {
            if ((mask & 1) == 0) {
                result += redValues.get(i);
            } else {
                result -= redValues.get(i);
            }
            mask >>>= 1;
        }
        return result;
    }
}
