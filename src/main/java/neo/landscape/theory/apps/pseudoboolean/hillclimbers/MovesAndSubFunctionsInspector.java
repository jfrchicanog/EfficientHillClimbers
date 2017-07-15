package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

public interface MovesAndSubFunctionsInspector {

    public double getSubFunctionEvaluation(int subFunction);
    public double getMoveImprovementByID(int id);

}