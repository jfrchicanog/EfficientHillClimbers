package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import org.junit.Test;

import java.util.Properties;
import java.util.stream.LongStream;

public class FourierPartitionCrossoverTest {

    @Test
    public void testFPX() {
        for (int N : new int [] {4, 5, 6, 7, 8, 9, 10, 50, 100, 500, 1000}) {
            LongStream.rangeClosed(1,10).forEach(seed-> {
                EmbeddedLandscape el = createNKLandscape(N, seed);
                WalshCoefficients wcs = WalshTransform.transform(el);
                WalshBasedFunction wbf = new WalshBasedFunction(el.getN(), wcs);
                FourierPartitionCrossover fpx = new FourierPartitionCrossover(wbf);
                fpx.setDebug(true);
                fpx.setPrintStream(System.out);

                for (int i=0; i < 10; i++) {
                    PBSolution solution = fpx.recombine(el.getRandomSolution(), el.getRandomSolution());
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
}
