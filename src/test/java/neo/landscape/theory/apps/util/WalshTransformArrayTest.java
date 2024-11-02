package neo.landscape.theory.apps.util;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class WalshTransformArrayTest {

    private static Stream<Arguments> provideParamsForNKLandscpaes() {
        return IntStream.rangeClosed(3, 12).mapToObj(
                N -> IntStream.rangeClosed(2, 5).mapToObj(K -> arguments(N, K)))
            .flatMap(x -> x);
    }

    @ParameterizedTest
    @MethodSource("provideParamsForNKLandscpaes")
    public void testWalshTransformArray(int N, int K) {
        // Given
        NKLandscapes pbf = getNkLandscapes(N, K);
        WalshCoefficients wcs = WalshTransform.transform(pbf, WalshCoefficients.factory());
        // When
        WalshCoefficientsArray wcsArray = WalshTransform.transform(pbf, WalshCoefficientsArray.factory());
        // Then
        assertThat(wcsArray.getNonzeroTerms()).isEqualTo(wcs.getNonzeroTerms());
        wcsArray.streamNonZeroCoefficients().forEach(c -> {
            Set<Integer> vars = wcsArray.getVarsForID(c).boxed().collect(Collectors.toSet());
            assertThat(wcsArray.getCoefficient(c)).isEqualTo(wcs.getCoefficient(vars));
            assertThat(wcsArray.getCoefficient(vars)).isEqualTo(wcs.getCoefficient(vars));
        });
    }

    private static NKLandscapes getNkLandscapes(int N, int K) {
        long seed = 2;

        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, String.valueOf(N));
        prop.setProperty(NKLandscapes.K_STRING, String.valueOf(K));
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        prop.setProperty(NKLandscapes.Q_STRING, "100");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }

    @ParameterizedTest
    @MethodSource("provideParamsForNKLandscpaes")
    public void testVariablesIndex(int N, int K) {
        // Given
        NKLandscapes pbf = getNkLandscapes(N, K);
        // When
        WalshCoefficientsArray wcsArray = WalshTransform.transform(pbf, WalshCoefficientsArray.factory());
        // Then
        wcsArray.streamNonZeroCoefficients().forEach(c -> {
            Set<Integer> vars = wcsArray.getVarsForID(c).boxed().collect(Collectors.toSet());
            assertThat(wcsArray.getIDForVariables(vars)).isEqualTo(c);
        });
    }

    @ParameterizedTest
    @MethodSource("provideParamsForNKLandscpaes")
    public void testCoefficientsPerVariable(int N, int K) {
        // Given
        NKLandscapes pbf = getNkLandscapes(N, K);
        // When
        WalshCoefficientsArray wcsArray = WalshTransform.transform(pbf, WalshCoefficientsArray.factory());
        // Then
        IntStream.range(0, N).forEach(v->{
            wcsArray.getCoefficientsPerVariable(v).forEach(c -> {
                Set<Integer> vars = wcsArray.getVarsForID(c).boxed().collect(Collectors.toSet());
                assertThat(vars).contains(v);
            });
        });

        wcsArray.streamNonZeroCoefficients().forEach(c -> {
            Set<Integer> vars = wcsArray.getVarsForID(c).boxed().collect(Collectors.toSet());
            vars.forEach(v -> {
                assertThat(wcsArray.getCoefficientsPerVariable(v).filter(c1 -> c1 == c).findFirst()).isPresent();
                });
            });
    }
}
