package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;

public class FourierPartitionCrossoverTest {

    @Test
    public void testFPX() {
        for (int N : new int [] {4, 5, 6, 7, 8, 9, 10, 50, 100, 500, 1000}) {
            LongStream.rangeClosed(1,10).forEach(seed-> {
                EmbeddedLandscape el = createNKLandscape(N, seed);
                WalshCoefficients wcs = WalshTransform.transform(el, WalshCoefficients.factory());
                WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
                FourierPartitionCrossover fpx = new FourierPartitionCrossover(wbf);
                fpx.setDebug(true);
                //fpx.setPrintStream(System.out);

                for (int i=0; i < 10; i++) {
                    PBSolution blue = el.getRandomSolution();
                    PBSolution red = el.getRandomSolution();
                    PBSolution solution = fpx.recombine(blue, red);
                    checkVariablesInComponents(fpx);
                    checkLattice(fpx, red);
                }
            });
        }
    }


    private NKLandscapes createNKLandscape(int N, long seed) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, ""+N);
        prop.setProperty(NKLandscapes.K_STRING, "2");
        prop.setProperty(NKLandscapes.Q_STRING, "64");
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, "random");

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }

    private void checkVariablesInComponents(FourierPartitionCrossover fpx) {
        List<Set<Integer>> varsInComponents = fpx.getVarsInComponents();
        fpx.getWcsConstrained().stream()
            .forEach(wc->{
                long [] oddCounts = varsInComponents.stream().mapToLong(varSet ->
                        varSet.stream().filter(wc.variables::contains).count()
                    )
                    .filter(l-> (l&1)!=0).toArray();

                assertThat(oddCounts.length).isLessThanOrEqualTo(1)
                    .withFailMessage("There are more than one odd count in component. Walsh variables "
                    +wc.variables+".\n"+"Counts: "+ Arrays.toString(oddCounts));
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
                assertThat(value).isEqualTo(expected, withPrecision(0.0001d));
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
