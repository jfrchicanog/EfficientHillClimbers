package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained.ConstrainedVectorMovesSelector.Region;

public interface HillClimberListener {
    public enum Reason {NO_IMPROVING_MOVE, NO_FEASIBLE_MOVE, 
        NO_IMPROVING_CONSTRAINT_MOVE, TIME_LIMIT};
    
    public void enterNewRegion(Region region, long totalMoves);
    public void reportSolutionQuality(double [] solutionQuality);
    public void finished(Reason reason, long totalMoves);

}
