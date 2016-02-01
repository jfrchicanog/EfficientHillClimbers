package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained;

public interface ConstrainedVectorMovesSelector<M> {

    public static enum Region {FEASIBLE, UNFEASIBLE};
    public static enum MoveClass {
        FEASIBLE, UNFEASIBLE, WFEASIBLE,
        WIMPROVING_F, DISIMPROVING_F, STRONG_IMPROVING_F,
        STRONG_IMPROVING_G, WIMPROVING_G, DISIMPROVING_G,
        STRONG_IMPROVING_Y, WIMPROVING_Y, DISIMPROVING_Y,
        FEASIBLE_STRONG_IMPROVING_Y, FEASIBLE_WIMPROVING_Y, FEASIBLE_DISIMPROVING_Y, 
        FEASIBLE_STRONG_IMPROVING_F, FEASIBLE_WIMPROVING_F, FEASIBLE_DISIMPROVING_F, 
        UNCLASSIFIED};
    
    public M getMoveByID(int id);
    public Iterable<M> allMoves();
    public int getNumberOfMoves();
    public void unclassifyMove(M move);
    public void assignBucketsToUnclassifiedMoves();
    public void checkCorrectPositionOfMovesInSelector();
    
    public void setWStar(double [] wstar);
    public void setYSolutionQuality(double [] yValue);
    public void setRegion(Region region);
    public M selectMove(MoveClass moveClass);
    
    

}
