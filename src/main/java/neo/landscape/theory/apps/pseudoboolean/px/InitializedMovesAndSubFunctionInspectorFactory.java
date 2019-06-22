package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionInspectorFactory;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionsInspector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

class InitializedMovesAndSubFunctionInspectorFactory
		implements MovesAndSubFunctionInspectorFactory {
	
	private RBallEfficientHillClimberSnapshot blue;
	private RBallEfficientHillClimberSnapshot red;
	private EmbeddedLandscape el;
	private VariableProcedence varProcedence;

	InitializedMovesAndSubFunctionInspectorFactory(RBallEfficientHillClimberSnapshot blue,
			RBallEfficientHillClimberSnapshot red, CrossoverInternal internal) {
		this.blue = blue;
		this.red = red;
		
		el = internal.getEmbddedLandscape();
		varProcedence = internal.getVarProcedence();
	}

	@Override
	public MovesAndSubFunctionsInspector getInspectorForSubFunction(int subFunction) {
	    int [][] masks = el.getMasks();
	    int color = VariableProcedence.PURPLE;
	    
	    for (int variable: masks[subFunction]) {
	        color &= varProcedence.getColor(variable);
	    }

	    return selectInspector(blue, red, color);
	}

	private MovesAndSubFunctionsInspector selectInspector(
	        final RBallEfficientHillClimberSnapshot blue,
	        final RBallEfficientHillClimberSnapshot red, int color) {
	    
	    if ((color & VariableProcedence.BLUE) != 0) {
	        //System.out.println("blue");
	        return blue;
	    } else if ((color & VariableProcedence.RED) != 0) {
	        //System.out.println("red");
	        return red;
	    } else {
	        //System.out.println("null");
	        return null;
	    }
	}

	@Override
	public MovesAndSubFunctionsInspector getInspectorForMove(SetOfVars setOfVars) {
	    
	    int color = VariableProcedence.PURPLE;
	    int [][] interactions = el.getInteractions();
	    for (int variable: setOfVars) {
	        color &= varProcedence.getColor(variable);
	        for (int adjacentVariable: interactions[variable]) {
	            color &= varProcedence.getColor(adjacentVariable);
	        }
	    }
	    //System.out.println("Moves");
	    return selectInspector(blue, red, color);
	    
	}
}