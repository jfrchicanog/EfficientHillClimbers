package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.io.Writer;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiObjectiveLandscape extends EmbeddedLandscape {
    private final EmbeddedLandscape[] landscapes;
    private final int numObjectives;

    // O(1) Lookup Tables for blazing fast evaluations
    private int[] landscapeMap;
    private int[] localSfMap;

    public MultiObjectiveLandscape(EmbeddedLandscape... landscapes) {
        if (landscapes == null || landscapes.length < 1) {
            throw new IllegalArgumentException("MultiObjectiveLandscape requires at least 1 embedded landscape.");
        }
        this.landscapes = landscapes;
        this.numObjectives = landscapes.length;
        initializaInternalData();
    }

    public int getNumObjectives() {
        return numObjectives;
    }

    /**
     * Sets the scaling weights for the underlying landscapes.
     */
    public void setWeights(double[] weights) {
        if (weights == null || weights.length != numObjectives) {
            throw new IllegalArgumentException("Weights array must have exactly " + numObjectives + " elements.");
        }
        for (int i = 0; i < numObjectives; i++) {
            landscapes[i].setAlpha(weights[i]);
        }
    }

    public double[] getWeights() {
        double[] weights = new double[numObjectives];
        for (int i = 0; i < numObjectives; i++) {
            weights[i] = landscapes[i].getAlpha();
        }
        return weights;
    }

    public double getWeight(int ObjectiveIndex) {
        return landscapes[ObjectiveIndex].getAlpha();
    }

    @Override
    public void setAlpha(double factor) {
        throw new UnsupportedOperationException("Use setWeights(double[]) for MultiObjectiveLandscape instead of setAlpha(double).");
    }

    @Override
    public double getAlpha() {
        throw new UnsupportedOperationException("Use getWeights() for MultiObjectiveLandscape instead of getAlpha().");
    }

    private boolean isSameN() {
        int firstN = landscapes[0].getN();
        return Stream.of(landscapes).skip(1).noneMatch(landscape -> landscape.getN() != firstN);
    }

    private void initializaInternalData() {
        if (!isSameN()) {
            throw new IllegalArgumentException("All problems in the MultiObjectiveLandscape must have the same N value.");
        }
        this.n = landscapes[0].getN();
        this.m = Stream.of(landscapes).mapToInt(EmbeddedLandscape::getM).sum();
        masks = new int[m][];

        // Initialize O(1) lookup tables
        landscapeMap = new int[m];
        localSfMap = new int[m];

        int subfunctionCount = 0;
        for (int l = 0; l < numObjectives; l++) {
            EmbeddedLandscape landscape = landscapes[l];
            for (int sf = 0; sf < landscape.getM(); sf++) {
                masks[subfunctionCount] = landscape.masks[sf].clone();

                // Map the global sub-function index to its specific landscape and local index
                landscapeMap[subfunctionCount] = l;
                localSfMap[subfunctionCount] = sf;

                subfunctionCount++;
            }
        }
    }

    @Override
    public void setConfiguration(Properties prop) {
    }

    @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        // O(1) Instant Lookup replacing the while loop
        return landscapes[landscapeMap[sf]].evaluateSubfunction(localSfMap[sf], pbs);
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        // O(1) Instant Lookup replacing the while loop
        return landscapes[landscapeMap[sf]].evaluateSubfunction(localSfMap[sf], value);
    }

    public void writeTo(Writer wr) {
        for (EmbeddedLandscape els : this.landscapes) {
            if (els instanceof NKLandscapes) {
                ((NKLandscapes) els).writeTo(wr);
            }
            if (els instanceof MAXSAT) {
                ((MAXSAT) els).writeTo(wr);
            }
        }
    }

    public List<Integer> getNValues() {
        return Stream.of(landscapes).mapToInt(EmbeddedLandscape::getN).boxed().collect(Collectors.toList());
    }

    public List<Integer> getMValues() {
        return Stream.of(landscapes).mapToInt(EmbeddedLandscape::getM).boxed().collect(Collectors.toList());
    }
}