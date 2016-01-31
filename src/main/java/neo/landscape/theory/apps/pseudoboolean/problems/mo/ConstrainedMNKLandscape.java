package neo.landscape.theory.apps.pseudoboolean.problems.mo;

import java.util.Properties;

public class ConstrainedMNKLandscape extends MNKLandscape {
    
    public static final String CONSTRAINTS_STRING="constraints"; 
    
    protected int constraintIndex;
    
    public ConstrainedMNKLandscape(long seed, Properties prop) {
        super(seed, prop);
        int constraints = 0;
        if (prop.getProperty(CONSTRAINTS_STRING) != null) {
            constraints = Integer.parseInt(prop.getProperty(CONSTRAINTS_STRING));
        }
        constraintIndex = this.getDimension() - constraints;
        if (constraintIndex < 1) {
            throw new IllegalArgumentException("At least one objective function must exist");
        }
    }
    
    

}
