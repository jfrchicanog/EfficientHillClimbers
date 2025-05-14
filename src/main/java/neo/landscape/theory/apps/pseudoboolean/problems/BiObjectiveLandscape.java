package neo.landscape.theory.apps.pseudoboolean.problems;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;

import java.io.Writer;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BiObjectiveLandscape extends EmbeddedLandscape {
    private EmbeddedLandscape[] landscapes;

    public BiObjectiveLandscape(EmbeddedLandscape... landscapes) {
        this.landscapes = landscapes;
        initializaInternalData();
    }

    @Override
    public void setAlpha(double factor) {
        landscapes[0].setAlpha(factor);
        landscapes[1].setAlpha(1 - factor);
    }

    private boolean isSameN() {
        int firstN = landscapes[0].getN();
        return Stream.of(landscapes).skip(1).noneMatch(landscape -> landscape.getN() != firstN);
    }

    private void initializaInternalData() {
        if (!isSameN()) {
            System.out.println("Both the problems must have same n value");
        }
        this.n = landscapes[0].getN();
        this.m = Stream.of(landscapes).mapToInt(EmbeddedLandscape::getM).sum();
        masks = new int[m][];
        int subfunctionCount = 0;
        for (EmbeddedLandscape landscape : landscapes) {
            for (int sf = 0; sf < landscape.getM(); sf++) {
                masks[subfunctionCount] = landscape.masks[sf].clone();
                subfunctionCount++;
            }
        }
    }

    @Override
    public void setConfiguration(Properties prop) {
    }

    @Override
    public double evaluateSubfunction(int sf, PBSolution pbs) {
        int l = 0;
        while (sf >= landscapes[l].getM()) {
            sf -= landscapes[l].getM();
            l++;
        }
        return landscapes[l].evaluateSubfunction(sf, pbs);
    }

    @Override
    public double evaluateSubfunction(int sf, int value) {
        int l = 0;
        while (sf >= landscapes[l].getM()) {
            sf -= landscapes[l].getM();
            l++;
        }
        return landscapes[l].evaluateSubfunction(sf, value);
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
