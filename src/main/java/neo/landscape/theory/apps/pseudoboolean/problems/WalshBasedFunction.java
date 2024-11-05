package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficient;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficientsInterface;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshConstraint;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.efficient.WalshCoefficientsArray;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class WalshBasedFunction<W extends WalshCoefficientsInterface<W>> extends EmbeddedLandscape implements KBoundedEpistasisPBF {
    public static final String INSTANCE_STRING = "instance";
    private W wcsOriginal;
    private int walshCoefficientIDs[];
    private int k;

    public WalshBasedFunction(int n, W wc) {
        super();
        this.n=n;
        wcsOriginal = wc;
        initializeBasicDataStructures();
    }

    public WalshBasedFunction(W wc) {
        super();
        wcsOriginal = wc;
        initializeBasicDataStructures();
        computeN();
    }

    public W getOriginalWalshTerms() {
        return wcsOriginal;
    }

    private void computeN() {
        n = 0;
        wcsOriginal.getNonZeroCoefficients().forEach(id -> {
            wcsOriginal.getVarsForID(id).forEach(v -> {
                if (v >= n) {
                    n = v+1;
                }
            });
        });
    }

    public WalshBasedFunction() {
        super();
    }

    private void initializeBasicDataStructures() {
        walshCoefficientIDs = wcsOriginal.getNonZeroCoefficients().toArray();
        m = walshCoefficientIDs.length;
        masks = new int[m][];
        k=0;
        for (int sf=0; sf < m; sf++) {
            masks[sf] = wcsOriginal.getVarsForID(walshCoefficientIDs[sf]).toArray();
            if (masks[sf].length > k) {
                k= masks[sf].length;
            }
        }
    }

    @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        int localK = wcsOriginal.numberOfVarsForID(walshCoefficientIDs[sf]);
        int sign = 0;
        for (int i = 0; i < localK; i++) {
            sign ^= pbs.getBit(i);
        }
        double value = wcsOriginal.getCoefficient(walshCoefficientIDs[sf]);
        return sign == 0? value: -value;
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        int localK = wcsOriginal.numberOfVarsForID(walshCoefficientIDs[sf]);
        int sign = Integer.bitCount(value & ((1 << localK)-1)) & 0x01;
        double coeff = wcsOriginal.getCoefficient(walshCoefficientIDs[sf]);
        return sign == 0? coeff: -coeff;
    }

    private void loadInstance(File file) {
        // Read the DIMCAS format
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader brd = new BufferedReader(isr)) {

            String line;
            String[] parts;


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
                        wcsOriginal = wcsOriginal.getFactory().create(n);
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

    public static <W extends WalshCoefficientsInterface<W>> WalshBasedFunction<W> inverseWalshTransform(W wcs) {
        return new WalshBasedFunction(wcs);
    }

    public W contraint(PBSolution red, PBSolution blue) {
        return WalshConstraint.constraint(wcsOriginal, red, blue);
    }
}