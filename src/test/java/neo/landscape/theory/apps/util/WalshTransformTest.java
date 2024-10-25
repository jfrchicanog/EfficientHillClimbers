package neo.landscape.theory.apps.util;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXkSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshConstraint;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.ref.SoftReference;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class WalshTransformTest {

    @Test
    public void testConstraint() {
        EmbeddedLandscape pbf = new MAXkSAT();
        pbf.setSeed(0L);
        Properties configuration = new Properties();
        configuration.setProperty("k", "3");
        configuration.setProperty("n", "5");
        configuration.setProperty("m", "1");
        pbf.setConfiguration(configuration);

        System.out.println("Problem:");
        System.out.println(pbf);

        WalshCoefficients wcs = WalshTransform.transform(pbf);
        System.out.println("Walsh Transform:");
        System.out.println(wcs);

        PBSolution red = PBSolution.toPBSolution("00000");
        PBSolution blue = PBSolution.toPBSolution("00001");
        WalshCoefficients newWc = WalshConstraint.constraint(wcs, red, blue);

        System.out.println("Walsh Transform after crossing " + red + " and " + blue);
        System.out.println(newWc);

        // PBSolution red = new PBSolution(10);
        // PBSolution blue = new PBSolution(10);
        // WalshCoefficients wc = new WalshCoefficients();
        // WalshCoefficients constraint = WalshConstraint.constraint(wc, red, blue);
        // assertEquals(0, constraint.size());
    }

    private static Stream<Arguments> provideParamsForNKLandscpaes() {
        return IntStream.rangeClosed(3, 12).mapToObj(
                N -> IntStream.rangeClosed(1, 2).mapToObj(K -> arguments(N, K)))
            .flatMap(x -> x);
    }

    @ParameterizedTest
    @MethodSource("provideParamsForNKLandscpaes")
    public void testWalshTransform(int N, int K) {
        long seed = 2;

        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, String.valueOf(N));
        prop.setProperty(NKLandscapes.K_STRING, String.valueOf(K));
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        prop.setProperty(NKLandscapes.Q_STRING, "100");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);

        WalshCoefficients wcs = WalshTransform.transform(pbf);
        EmbeddedLandscape el = WalshBasedFunction.inverseWalshTransform(wcs);

        for (int x = 0; x < 1 << N; x++) {
            PBSolution pbs = PBSolution.readFromInt(N, x);
            double v1 = pbf.evaluate(pbs);
            double v2 = el.evaluate(pbs);

            assertThat(v1).isEqualTo(v2).withFailMessage("The functions are not the same at " + pbs.toString());
        }
    }
}
