package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficient;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshConstraint;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class WalshBasedFunction extends EmbeddedLandscape implements KBoundedEpistasisPBF {
    public static final String INSTANCE_STRING = "instance";
    private WalshCoefficient [] wcs;
    private WalshCoefficients wcsOriginal;
    private int k;

    public WalshBasedFunction(int n, WalshCoefficients wc) {
        super();
        this.n=n;
        wcsOriginal = wc;
        initializeBasicDataStructures();
    }

    public WalshBasedFunction(WalshCoefficients wc) {
        super();
        wcsOriginal = wc;
        initializeBasicDataStructures();
        computeN();
    }

    private void computeN() {
        n = wcsOriginal.stream()
            .mapToInt(wc->wc.variables.stream()
                .mapToInt(l->l).max().orElse(0))
            .max().orElse(-1)+1;
    }

    public WalshBasedFunction() {
        super();
    }

    private void initializeBasicDataStructures() {
        wcs = wcsOriginal.stream().collect(Collectors.toList()).toArray(new WalshCoefficient[0]);
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

    private void loadInstance(File file) {
        // Read the DIMCAS format
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader brd = new BufferedReader(isr)) {

            String line;
            String[] parts;

            wcsOriginal = new WalshCoefficients();
            while ((line = brd.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // else
                switch (line.charAt(0)) {
                    case 'c': // A comment, skip it
                        break;
                    case 'p': // Instance information
                        n = Integer.parseInt(line.substring(1).trim());
                        break;
                    case 'w':
                        parts = line.split(":");
                        if (parts.length != 2) {
                            throw new IllegalArgumentException("Unrecognized line format: "+line);
                        }

                        Set<Integer> vars = Arrays.stream(parts[0].substring(1).trim().split(" +"))
                            .filter(s->s.length()>0)
                            .map(Integer::parseInt).collect(Collectors.toSet());
                        double value = Double.parseDouble(parts[1].trim());
                        wcsOriginal.addCoefficient(vars, value);
                        break;
                    default: // Not recognized
                        throw new IllegalArgumentException("Unrecognized line command: "+line.charAt(0));
                }
            }

            initializeBasicDataStructures();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setConfiguration(Properties prop) {
        if (prop.getProperty(INSTANCE_STRING) != null) {
            loadInstance(new File(prop.getProperty(INSTANCE_STRING)));
        }
    }

    @Override
    public int getK() {
        return k;
    }

    public static WalshBasedFunction inverseWalshTransform(WalshCoefficients wcs) {
        return new WalshBasedFunction(wcs);
    }

    public WalshCoefficients contraint(PBSolution red, PBSolution blue) {
        return WalshConstraint.constraint(wcsOriginal, red, blue);
    }

    public WalshCoefficients getWalshCoefficients(){
        return wcsOriginal;
    }
}