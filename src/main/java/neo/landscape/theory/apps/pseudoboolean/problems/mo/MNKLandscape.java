package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import java.util.Properties;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class MNKLandscape extends VectorMKLandscape {
    
    public static final String DIMENSION_STRING = "d";
    
    protected Random random;
    
    
    public MNKLandscape(long seed, Properties configuration) {
        setSeed(seed);
        setConfiguration(configuration);
    }
    
    @Override
    public void setSeed(long seed) {
        random = new Random(seed);
    }

    @Override
    public void setConfiguration(Properties prop) {
        int dimension = Integer.parseInt(prop.getProperty(DIMENSION_STRING));
        
        NKLandscapes [] objectives = new NKLandscapes[dimension];
        for (int i = 0; i < objectives.length; i++) {
            objectives[i] = new NKLandscapes();
            objectives[i].setSeed(random.nextLong());
            objectives[i].setConfiguration(prop);
        }
        configureEmbeddedLandscapes(objectives);
    }

}
