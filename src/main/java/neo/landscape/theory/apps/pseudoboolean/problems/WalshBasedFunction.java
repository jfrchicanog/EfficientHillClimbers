package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficient;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;

import java.util.Properties;
import java.util.stream.Collectors;

public class WalshBasedFunction extends EmbeddedLandscape implements KBoundedEpistasisPBF {
    private WalshCoefficient [] wcs;
    private int k;

    public WalshBasedFunction(WalshCoefficients wc) {
        super();
        wcs = wc.stream().collect(Collectors.toList()).toArray(new WalshCoefficient[0]);
        initializeBasicDataStructures();
    }

    private void initializeBasicDataStructures() {
        m = wcs.length;
        masks = new int[m][];
        k=0;
        for (int sf=0; sf < m; sf++) {
            masks[sf] = wcs[sf].variables.stream().mapToInt(Integer::intValue).toArray();
            if (masks[sf].length > k) {
                k= masks[sf].length;
            }
        }
    }

    @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        WalshCoefficient wc = wcs[sf];
        int localK = wc.variables.size();
        int sign = 0;
        for (int i = 0; i < localK; i++) {
            sign ^= pbs.getBit(i);
        }
        return sign == 0? wc.value: -wc.value;
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        WalshCoefficient wc = wcs[sf];
        int localK = wc.variables.size();
        int sign = Integer.bitCount(value & ((1 << localK)-1)) & 0x01;
        return sign == 0? wc.value: -wc.value;
    }

    @Override
    public void setConfiguration(Properties prop) {
        throw new UnsupportedOperationException("This initialization method is not supported yet");
    }

    @Override
    public int getK() {
        return k;
    }

    public static WalshBasedFunction inverseWalshTransform(WalshCoefficients wcs) {
        return new WalshBasedFunction(wcs);
    }
}