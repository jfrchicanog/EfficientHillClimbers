package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class VectorMKSubfunctionTranslator {
    private int[] subfunctionsMapping;
    private int dimension;

    public VectorMKSubfunctionTranslator(EmbeddedLandscape[] embeddedLandscapes) {
        dimension = embeddedLandscapes.length;
        computeSubfunctionMapping(embeddedLandscapes);
    }

    private void computeSubfunctionMapping(EmbeddedLandscape[] embeddedLandscapes) {
        subfunctionsMapping = new int [embeddedLandscapes.length+1];
        int m=0;
        for (int i = 0; i < embeddedLandscapes.length; i++) {
            subfunctionsMapping[i] = m;
            m += embeddedLandscapes[i].getM();
        }
        subfunctionsMapping[embeddedLandscapes.length]=m;
        
    }

    public int dimensionOfSunbfunctionID(int subfunctionID) {
        int dimension=0;
        while (subfunctionsMapping[dimension+1] <= subfunctionID) {
            dimension++;
        }
        assert dimension < subfunctionsMapping[this.dimension];
        return dimension;
    }

    public int subfunctionOfSubfunctionID(int subfunctionID) {
        int dimension = dimensionOfSunbfunctionID(subfunctionID);
        return subfunctionID-subfunctionsMapping[dimension];
    }

    public int subfunctionID(int dimension, int subfunctionInDimension) {
        int result = subfunctionsMapping[dimension] + subfunctionInDimension;
        assert result < subfunctionsMapping[dimension+1];
        return result;
    }

    public int getM() {
        return subfunctionsMapping[dimension];
    }
}