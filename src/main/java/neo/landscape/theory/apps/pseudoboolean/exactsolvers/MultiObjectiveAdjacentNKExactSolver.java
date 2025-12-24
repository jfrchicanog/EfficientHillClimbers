package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKSubfunctionTranslator;
import neo.landscape.theory.apps.pseudoboolean.util.IParetoNonDominatedSet;
import neo.landscape.theory.apps.pseudoboolean.util.IParetoNonDominatedSetFactory;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class MultiObjectiveAdjacentNKExactSolver<NS extends IParetoNonDominatedSet<NS>> {


    private VectorMKLandscape vectorMKLandscape;
    private IParetoNonDominatedSetFactory<NS> paretoNonDominatedSetFactory;
    private NS [][] paretoNSSets1;
    private NS [][] paretoNSSets2;
    private int n;
    private int k;
    private int d;
    private VectorMKSubfunctionTranslator translator;
    private PBSolution solution;
    private int mask;
    private ExecutorService pool;
    private Optional<PrintWriter> pw = Optional.empty();
    private long startTime;

    public MultiObjectiveAdjacentNKExactSolver(IParetoNonDominatedSetFactory<NS> factory) {
        paretoNonDominatedSetFactory = factory;
        int nbThreads = Runtime.getRuntime().availableProcessors();
        pool =  Executors. newFixedThreadPool (1);
    }

    public MultiObjectiveAdjacentNKExactSolver(IParetoNonDominatedSetFactory<NS> factory, PrintWriter pw) {
        this(factory);
        this.pw = Optional.ofNullable(pw);
    }


    private void checkVectorMKLandscape() {
        if (d != 2) {
            throw new IllegalArgumentException("Only 2 objectives are supported");
        }
        if (k > 31) {
            throw new IllegalArgumentException("The mask length is too big");
        }
        if (vectorMKLandscape.getConstraintIndex() < d) {
            throw new IllegalArgumentException("Constraints are not supported");
        }
        int m = vectorMKLandscape.getM();
        if (!IntStream.range(0, d).allMatch(dim -> translator.subfunctionID(dim, 0)==dim*n)){
            throw new IllegalArgumentException("The number of subfunctions is not n in some dimension");
        }
        IntStream.range(0, m).forEach(sf -> {
            if (vectorMKLandscape.getMaskLength(sf) != k) {
                throw new IllegalArgumentException("All subfunctions must have the same mask length");
            }
            var setOfVars = IntStream.range(0, k)
                                .map(i->vectorMKLandscape.getMasks(sf, i))
                                .boxed()
                                .toList();
            int findex = translator.subfunctionOfSubfunctionID(sf);
            var target = IntStream.range(0, k)
                .map(i->(findex+i) % n)
                .boxed()
                .toList();
            if (!setOfVars.equals(target)) {
                throw new IllegalArgumentException("The subfunctions are not arranged in an adjacent manner");
            }
        });
    }

    private void initializeDataStructures() {
        paretoNSSets1  = initializeParetoNSSets();
        paretoNSSets2  = initializeParetoNSSets();
    }

    private NS [][] initializeParetoNSSets() {
        int limit = 1 << (k-1);
        NS [][] result = (NS [][])Array.newInstance(paretoNonDominatedSetFactory.create().getClass(), limit, limit);
        for (int i = 0; i < limit; i++) {
            for (int j = 0; j < limit; j++) {
                result[i][j] = paretoNonDominatedSetFactory.create();
            }
        }
        return result;
    }

    private void reportMessage(Supplier<String> message) {
        pw.ifPresent(pw -> pw.println(message.get()));
    }

    public NS computeParetoFront(VectorMKLandscape vectorMKLandscape) {
        startTime = System.nanoTime();
        this.vectorMKLandscape = vectorMKLandscape;
        translator = vectorMKLandscape.getSubfunctionsTranslator();
        n = vectorMKLandscape.getN();
        d = vectorMKLandscape.getDimension();
        k = vectorMKLandscape.getMaskLength(0);
        mask = (1 << k) -1;
        solution = new PBSolution (k);
        int K=k-1;

        checkVectorMKLandscape();
        if (n <= 2 * K) {
            return applyExhaustiveEnumeration();
        }

        if (K==0) {
            return solveAdditiveDecomposable();
        }

        initializeDataStructures();
        computeFirstFunction(paretoNSSets1);
        NS [][] computed = paretoNSSets1;
        NS [][] target = paretoNSSets2;

        int subfunction;
        for (subfunction=K; subfunction < n-K; subfunction++) { // stop when there K subfunctions missing
            // Absorb subfunction K and eliminate variable K (subfunction K goes from var K to 2K)
            if ((subfunction % 1000) == 0) {
                final int sf = subfunction;
                reportMessage(()-> String.format("Eliminating subfunction %1d at time %2d (ms)",sf, (System.nanoTime()-startTime)/1_000_000L));
            }
            eliminateVariable(subfunction, computed, target);
            NS [][] tmp = computed;
            computed = target;
            target = tmp;
        }
        // Then do exhaustive search using also the remaining subfunctions.
        return exhaustiveSearchWithRemainingFunctions(computed);
    }

    private NS solveAdditiveDecomposable() {
        NS accumulated = paretoNonDominatedSetFactory.create();
        NS result = paretoNonDominatedSetFactory.create();
        NS auxiliary = paretoNonDominatedSetFactory.create();
        double [] offset = new double[d];
        solution = new PBSolution(1);

        for (int sf=0; sf < n; sf++) {
            auxiliary.clear();
            for (int bit=0; bit < 2; bit++) {
                solution.getData()[0] = bit;
                double [] vector = new double[d];
                for (int dim=0; dim < d; dim++) {
                    int inner_sf = translator.subfunctionID(dim, sf);
                    vector[dim] += vectorMKLandscape.evaluateSubfunction(inner_sf, solution);
                }
                auxiliary.addPoint(vector);
            }
            if (auxiliary.size() == 1) {
                // Just one point, accumulate the offset
                double [] point = auxiliary.stream().findFirst().get();
                for (int dim=0; dim < d; dim++) {
                    offset[dim] += point[dim];
                }
            } else {
                double [] point1 = auxiliary.stream().findFirst().get();
                double [] point2 = auxiliary.stream().skip(1).findFirst().get();
                if (accumulated.size() == 0) {
                    accumulated.addPoint(new double[d]);
                }
                paretoNonDominatedSetFactory.combine(accumulated, point1, accumulated, point2, result);
                NS tmp = accumulated;
                accumulated = result;
                result = tmp;
            }
        }
        auxiliary.clear();
        paretoNonDominatedSetFactory.combine(accumulated, offset, auxiliary, new double[d], result);
        return result;
    }

    private NS exhaustiveSearchWithRemainingFunctions(NS[][] computed) {
        int K = k-1;
        int subfunction;
        NS accumulated = paretoNonDominatedSetFactory.create();
        NS result = paretoNonDominatedSetFactory.create();

        double [] zero = new double[d];
        double [] offset = new double[d];
        int limit = 1 << K;
        for (int i=0; i < limit; i++) {
            for (int j=0; j < limit; j++) {
                // Compute the offset using the subfunctions
                initializePoint(offset);
                for (subfunction=n- K; subfunction < n; subfunction++) {
                    solution.getData()[0] = (int)(((((long)j << K) | i) >>> (subfunction-(n- K))) & mask);
                    for (int dim=0; dim < d; dim++) {
                        int inner_sf = translator.subfunctionID(dim, subfunction);
                        offset[dim] += vectorMKLandscape.evaluateSubfunction(inner_sf, solution);
                    }
                }
                // Accumulate
                paretoNonDominatedSetFactory.combine(computed[i][j], offset, accumulated, zero, result);
                NS tmp = accumulated;
                accumulated = result;
                result = tmp;
            }
        }
        return accumulated;
    }

    private NS applyExhaustiveEnumeration() {
        if (n > 31) {
            throw new IllegalArgumentException("Exhaustive enumeration is only supported for n <= 31");
        }
        NS result = paretoNonDominatedSetFactory.create();
        int limit = 1 << n;
        solution = new PBSolution(n);
        for (int i=0; i < limit; i++) {
            solution.getData()[0] = i;
            result.addPoint(vectorMKLandscape.evaluate(solution));
        }
        return result;
    }

    private void eliminateVariable(int subfunction, NS[][] computed, NS[][] target) {
        int limit = 1<< (k-1);
        // pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        double [][] point = new double[2][d];
        for (int j=0; j < limit; j++) {
            for (int i = 0; i < limit; i++) {
                final int fi = i;
                final int fj = j;
                // pool.submit( () -> {
                    // double [][] point = new double[2][d];
                    initializePoints(point);
                    PBSolution solution = new PBSolution (k);
                    int msb_index = (fi << 1);
                    for (int lsb_bit = 0; lsb_bit < 2; lsb_bit++) {
                        solution.getData()[0] = msb_index | lsb_bit;
                        for (int dim=0; dim < d; dim++) {
                            int inner_sf = translator.subfunctionID(dim, subfunction);
                            point[lsb_bit][dim] += vectorMKLandscape.evaluateSubfunction(inner_sf, solution);
                        }
                    }
                    int masked_msb = msb_index & (limit-1);
                    paretoNonDominatedSetFactory.combine(
                        computed[masked_msb][fj], point[0],
                        computed[masked_msb | 0x1][fj],
                        point[1], target[fi][fj]);
                // });


            }
        }
        /* pool.shutdown();
        try {
            pool.awaitTermination(1000, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
    }

    private void initializePoints(double[][] point) {
        for (int i=0; i < point.length; i++) {
            initializePoint(point[i]);
        }
    }

    private void initializePoint(double[] point) {
        for (int dim=0; dim < d; dim++) {
            point[dim] = 0.0;
        }
    }

    private void computeFirstFunction(NS [][] paretoNSSets) {
        int K=k-1;
        int limit = 1<< K;
        for (int i=0; i < limit; i++) {
            for (int j=0; j < limit; j++) {
                double [] point = new double[d];
                for (int sf = 0; sf < K; sf++) {
                    solution.getData()[0] = (int)(((((long)i) << K) | j) >>> sf) & mask;
                    for (int dim=0; dim < d; dim++) {
                        int inner_sf = translator.subfunctionID(dim, sf);
                        point[dim] += vectorMKLandscape.evaluateSubfunction(inner_sf, solution);
                    }
                }
                paretoNSSets[i][j].addPoint(point);
            }
        }
    }


}
