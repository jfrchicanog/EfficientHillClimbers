package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public interface MovesAndSubFunctionInspectorFactory {
    public MovesAndSubFunctionsInspector getInspectorForMove(SetOfVars setOfVars);
    public MovesAndSubFunctionsInspector getInspectorForSubFunction(int subFunction);

}
