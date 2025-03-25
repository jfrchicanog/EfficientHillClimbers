package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.PBSolution.BitsOrder;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProg;
import neo.landscape.theory.apps.pseudoboolean.perturbations.EmbeddedLandscapeSubfunctionMaximizer;
import neo.landscape.theory.apps.pseudoboolean.perturbations.SolutionModifierAndSpy;
import neo.landscape.theory.apps.util.Seeds;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NKLandscapes extends EmbeddedLandscape implements
        KBoundedEpistasisPBF, EmbeddedLandscapeSubfunctionMaximizer {

    public static enum NKModel {ADJACENT, LOCALIZED, RANDOM};

    public static final String N_STRING = "n";
    public static final String FACTOR = "alpha";
    public static final String K_STRING = "k";
    public static final String Q_STRING = "q";
    public static final String SHIFT_STRING = "s";
    public static final String CIRCULAR_STRING = "circular";
    public static final String FORCE_NK = ".";
    public static final String IS_SAT = "is_sat";
    public static final String SAT_STEP = "sat_step";


    protected double[][] subFunctions;
    protected int[] maxIndexInSubFnction;
    protected int q;
    protected int shift;
    protected NKModel nkModel;
    protected int k;
    protected double alpha;
    protected boolean isSat;
    protected double sat_step = 1.0;

    @Override
    public void setConfiguration(Properties prop) {

        n = Integer.parseInt(prop.getProperty(N_STRING));
        k = Integer.parseInt(prop.getProperty(K_STRING)) + 1;
        if (k > n) {
            throw new IllegalArgumentException("K must be strictly less than N");
        }
        m = n;

        int twoToK = 1 << k;
        q = twoToK;

        if (prop.getProperty(FACTOR) != null) {
            alpha = Double.parseDouble(prop.getProperty(FACTOR));
        } else {
            alpha = 1.0;
        }

        isSat = prop.containsKey(IS_SAT);
        if (isSat) {
            m = (int) (4.27 * n);
        }

        if (prop.containsKey(SAT_STEP)) {
            sat_step = Double.parseDouble(prop.getProperty(SAT_STEP));
        }

        if (prop.getProperty(Q_STRING) != null) {
            if (prop.getProperty(Q_STRING).equals(FORCE_NK)) {
                // Generate an NK-landscape instead of NKq-landscape
                q = -1;
            } else {
                q = Integer.parseInt(prop.getProperty(Q_STRING));
                if (prop.getProperty(SHIFT_STRING) != null) {
                    shift = Integer.parseInt(prop.getProperty(SHIFT_STRING));
                }
            }
        }

        int window = n - 1;
        nkModel = NKModel.RANDOM;
        if (prop.getProperty(CIRCULAR_STRING) != null) {
            if (prop.getProperty(CIRCULAR_STRING).equals("yes")
                    || prop.getProperty(CIRCULAR_STRING).equals("adjacent")) {
                nkModel = NKModel.ADJACENT;
            } else if (prop.getProperty(CIRCULAR_STRING).equals("random")) {
                window = n - 1;
                nkModel = NKModel.RANDOM;
            } else {
                try {
                    window = Integer.parseInt(prop.getProperty(CIRCULAR_STRING));
                    if (window < k - 1 || window > n - 1) {
                        throw new RuntimeException("Invalid NK localized model: " + prop.getProperty(CIRCULAR_STRING));
                    } else if (window == k - 1) {
                        nkModel = NKModel.ADJACENT;
                    } else if (window == n - 1) {
                        nkModel = NKModel.RANDOM;
                    } else {
                        nkModel = NKModel.LOCALIZED;
                    }

                } catch (NumberFormatException e) {
                    throw new RuntimeException("NK model unrecognized: " + prop.getProperty(CIRCULAR_STRING));
                }
            }
        }

        subFunctions = new double[m][twoToK];
        masks = new int[m][k];
        maxIndexInSubFnction = new int[m];

        // Initialize masks and subfunctions

        if (NKModel.ADJACENT.equals(nkModel)) {
            initializeMasksCircular();
        } else {
            initializeMasksNonCircular(window);
        }

        // Initialize subfunctions
        initializeSubfunctions();
    }

    private void initializeSubfunctions() {
        int twoToK = 1 << k;
        List<Double> numbers = new ArrayList<>();
        if (isSat) {
            // Generate numbers (1 to n-1 and 400)
//			numbers = Stream.concat(
//							(twoToK > 1) ? IntStream.range(1, twoToK).mapToDouble(i -> i * alpha).boxed()
//									: Stream.<Double>empty(), // Empty stream if twoToK <= 1
//							Stream.of(((alpha * 100) / (1 - alpha)) * alpha) // Always include 400 * alpha
//					)
//					.map(i -> Math.round(i * 100.0) / 100.0) // Round to 2 decimal places
//					.collect(Collectors.toList());

            numbers = Stream.concat(
                            (twoToK > 1) ? IntStream.range(1, twoToK).mapToDouble(i -> sat_step * alpha).boxed()
                                    : Stream.of(sat_step * alpha),
                            Stream.of(0.0) // Single zero added
                    )
                    .collect(Collectors.toList());

        }

        for (int sf = 0; sf < m; sf++) {
            int maxIndex = 0;
            if (isSat) {
                // Shuffle the list
                Collections.shuffle(numbers);
                subFunctions[sf] = numbers.stream().mapToDouble(Double::doubleValue).toArray();
            } else {
                // Generate random numbers and find max index
                subFunctions[sf] = IntStream.range(0, twoToK)
                        .mapToDouble(i -> alpha * ((q > 0) ? rnd.nextInt(q) - shift : rnd.nextDouble()))
                        .toArray();
            }

            for (int i = 0; i < twoToK; i++) {
                if (subFunctions[sf][i] > subFunctions[sf][maxIndex]) {
                    maxIndex = i;
                }
            }
            maxIndexInSubFnction[sf] = maxIndex;
        }
    }

    private void initializeMasksCircular() {
        for (int sf = 0; sf < m; sf++) {
            // Initialize masks
            masks[sf][0] = sf % n;
            for (int i = 1; i < k; i++) {
                masks[sf][i] = (sf + i) % n;
            }

        }
    }

    private void initializeMasksNonCircular(int window) {
        if (window == n - 1) {
            oldWayToBuildRandomModel();
        } else {
            int[] aux = new int[window];
            for (int i = 0; i < window; i++) {
                aux[i] = i;
            }

            for (int sf = 0; sf < m; sf++) {
                // Shuffle the aux array to get k-1 random values from the window
                // values.
                for (int i = 0; i < k - 1; i++) {
                    int r = i + rnd.nextInt(window - i);
                    int v = aux[i];
                    aux[i] = aux[r];
                    aux[r] = v;
                }

                masks[sf][0] = sf;
                for (int i = 1; i < k; i++) {
                    masks[sf][i] = (aux[i - 1] + 1 + sf) % n;
                }
            }
        }

    }

    private void oldWayToBuildRandomModel() {
        int[] aux = new int[n - 1];
        for (int i = 0; i < n - 1; i++) {
            aux[i] = i;
        }

        for (int sf = 0; sf < m; sf++) {
            // Initialize masks

            masks[sf][0] = sf;
            // Shuffle the aux array to get k-1 random values from the n-1
            // values.
            for (int i = 0; i < k - 1; i++) {
                int r = i + rnd.nextInt(n - 1 - i);
                int v = aux[i];
                aux[i] = aux[r];
                aux[r] = v;
            }

            // Copy the other variables into the mask
            for (int i = 1; i < k; i++) {
                int v = aux[i - 1];
                if (v == sf) {
                    masks[sf][i] = n - 1;
                } else {
                    masks[sf][i] = v;
                }
            }

        }
    }

    public int getQ() {
        return q;
    }

    @Override
    public double evaluate(Solution sol) {

        PBSolution pbs = (PBSolution) sol;

        if (subFunctions == null) {
            throw new IllegalStateException(
                    "The NK-landscape has not been configured");
        }

        double res = 0;

        for (int sf = 0; sf < m; sf++) {
            int index = 0;
            for (int i = k - 1; i >= 0; i--) {
                index = (index << 1) + pbs.getBit(masks[sf][i]);
            }
            res += subFunctions[sf][index];
        }

        return res;
    }

    public BigDecimal evaluateArbitraryPrecision(Solution sol) {
        PBSolution pbs = (PBSolution) sol;

        if (subFunctions == null) {
            throw new IllegalStateException(
                    "The NK-landscape has not been configured");
        }

        BigDecimal res = BigDecimal.ZERO;

        for (int sf = 0; sf < m; sf++) {
            int index = 0;
            for (int i = k - 1; i >= 0; i--) {
                index = (index << 1) + pbs.getBit(masks[sf][i]);
            }
            res = res.add(new BigDecimal(subFunctions[sf][index]));
        }

        return res;
    }

    @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        if (subFunctions == null) {
            throw new IllegalStateException(
                    "The NK-landscape has not been configured");
        }

        int index = 0;
        for (int i = k - 1; i >= 0; i--) {
            index = (index << 1) + pbs.getBit(i);
        }

        return subFunctions[sf][index];
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        if (subFunctions == null) {
            throw new IllegalStateException(
                    "The NK-landscape has not been configured");
        }
        return subFunctions[sf][value];
    }

    public static void showHelp() {
        System.err.println("Arguments: <N> <K> <q> <c> [<shift>] [<seed>] [<solution>]");
        System.err.println("Use <q>=" + FORCE_NK
                + " for NK landscapes (otherwise NKq-landscape is generated)");
        System.err.println("Use <q>=- for q=2^(K+1)");
        System.err
                .println("<c> hould be yes (for adjacent-model) or no (for random-model)");
        System.err
                .println("The instance is written in the standard output (if no solution is given)");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            showHelp();
            return;
        }

        String n = args[0];
        String k = args[1];
        String q = args[2];
        String circular = args[3];
        PBSolution sol = null;
        long seed = 0;
        String shift = "0";
        boolean readFromReader = false;

        if (args.length >= 5) {
            shift = args[4];
        }

        if (args.length >= 6) {
            seed = Long.parseLong(args[5]);
        } else {
            seed = Seeds.getSeed();
        }

        if (args.length >= 7) {
            String solution = args[6];
            if (solution.equals("-")) {
                readFromReader = true;
            } else {
                sol = PBSolution.toPBSolution(solution);
            }
        }

        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
        if (!q.equals("-")) {
            prop.setProperty(NKLandscapes.Q_STRING, q);
            prop.setProperty(NKLandscapes.SHIFT_STRING, shift);
        }

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);

        if (sol == null && !readFromReader) {
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            pbf.writeTo(writer);
            if (NKModel.ADJACENT.equals(pbf.getNKModel())) {
                pbf.briandGoldmanFormat(writer);
            }
            writer.close();
        } else if (readFromReader) {
            BufferedReader brd = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = brd.readLine()) != null) {
                String[] qualitySolution = line.split(" ");
                double quality = Double.parseDouble(qualitySolution[0]);
                sol = PBSolution.toPBSolution(qualitySolution[1], BitsOrder.LITTLE_ENDIAN);
                double actualQuality = pbf.evaluate(sol);
                if (actualQuality != quality) {
                    System.out.println("Wrong value: expected " + quality + " but was " + actualQuality);
                }
            }

            brd.close();
        } else {
            BigDecimal res = pbf.evaluateArbitraryPrecision(sol);
            System.out.println("Arbitrary precision optimal value: " + res);
            System.out.println("Double: " + res.doubleValue());
        }

    }

    public NKModel getNKModel() {
        return nkModel;
    }

    public double[][] getSubFunctions() {
        return subFunctions;
    }

    public int getK() {
        return k;
    }

    public void briandGoldmanFormat(Writer wr) {
        PrintWriter pw = new PrintWriter(wr);

        NKLandscapesCircularDynProg solver = new NKLandscapesCircularDynProg(-1.0);
        SolutionQuality<NKLandscapes> solution = solver.solveProblem(this);

        pw.println((int) solution.quality + " " + ((PBSolution) solution.solution).printReversed());

        solver = new NKLandscapesCircularDynProg(1.0);
        solution = solver.solveProblem(this);

        pw.println((int) solution.quality + " " + ((PBSolution) solution.solution).printReversed());

        for (int i = 0; i < n; i++) {
            // Write the subfunction values
            for (int j = 0; j < subFunctions[i].length; j++) {
                int val = (int) subFunctions[i][reverseBits(j, k)];
                pw.print(val + " ");
            }
            pw.println();
        }

    }

    private int reverseBits(int j, int k) {
        int result = 0;
        for (; k > 0; k--) {
            result <<= 1;
            result |= (j & 0x01);
            j >>>= 1;
        }
        return result;
    }

    public void writeTo(Writer wr) {
        PrintWriter pw = new PrintWriter(wr);

        pw.println("===============================================================================================");
        pw.println("c NK-landscape instance");
        pw.println("c Generated by " + getClass().getName());
        pw.println("c The next line contains N, K and Q");
        pw.println("p NK " + n + " " + (k - 1) + " " + q);
        pw.println("c For each subfunction first it appears the mask in a line starting with 'm'");
        pw.println("c The mask is a list of K+1 values with the indices of the variables in Big Endian order");
        pw.println("c Following the mask the subfunction is defined in a line without prefix");
        pw.println("c Random seed: " + seed);

        for (int i = 0; i < n; i++) {
            // Write the mask
            pw.print("m ");
            for (int j = k - 1; j >= 0; j--) {
                pw.print(masks[i][j] + " ");
            }
            pw.println();

            // Write the subfunction values
            for (int j = 0; j < subFunctions[i].length; j++) {
                pw.print(subFunctions[i][j] + " ");
            }
            pw.println();
        }
        pw.println("===============================================================================================");
        pw.flush();

    }

    public static void oldMain(String[] args) {

        if (args.length < 2) {
            System.out.println("Arguments: <n> <k> [<seed>]");
            return;
        }

        String n = args[0];
        String k = args[1];
        long seed = 0;

        if (args.length > 2) {
            seed = Long.parseLong(args[2]);
        }

        NKLandscapes nkl = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);
        nkl.setSeed(seed);
        nkl.setConfiguration(prop);

        int[][] ai = nkl.getAppearsIn();

        int min = ai[0].length;
        int max = ai[0].length;
        double sum = ai[0].length;

        int[] histogram = new int[ai.length];

        histogram[ai[0].length]++;

        for (int i = 1; i < ai.length; i++) {

            histogram[ai[i].length]++;
            sum += ai[i].length;

            if (ai[i].length > max) {
                max = ai[i].length;
            }

            if (ai[i].length < min) {
                min = ai[i].length;
            }
        }

        System.out.println("Min:" + min);
        System.out.println("Max:" + max);
        System.out.println("Avg:" + sum / ai.length);

        int p;
        int q;

        for (p = 0; p < ai.length && histogram[p] == 0; p++)
            ;
        for (q = ai.length - 1; q >= 0 && histogram[q] == 0; q--)
            ;

        System.out.println("Histogram [" + p + ".." + q + "]:"
                + Arrays.toString(Arrays.copyOfRange(histogram, p, q + 1)));

    }

    @Override
    public void maximiceSubfunction(int subfunction, SolutionModifierAndSpy solutionModifier) {
        int index = (int) solutionModifier.getValuesForVariables(masks[subfunction]);
        if (index == maxIndexInSubFnction[subfunction] ||
                subFunctions[subfunction][index] >= subFunctions[subfunction][maxIndexInSubFnction[subfunction]]) {
            return;
        }

        int difference = (index ^ maxIndexInSubFnction[subfunction]);
        for (int variable : masks[subfunction]) {
            if ((difference & 1) == 1) {
                solutionModifier.flip(variable);
            }
            difference >>>= 1;
        }
    }

}
