package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionInspectorFactory;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionsInspector;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class DPXForRBallHillClimber extends DynasticPotentialCrossover {

	public DPXForRBallHillClimber(EmbeddedLandscape el) {
		super(el);
	}

	public RBallEfficientHillClimberSnapshot recombine(
			final RBallEfficientHillClimberSnapshot blue,
			final RBallEfficientHillClimberSnapshot red) {
	    
	    long initTime = System.nanoTime();
	    
		PBSolution blueSolution = blue.getSolution();
		PBSolution redSolution = red.getSolution();
		PBSolution res = super.recombine(blueSolution, redSolution);

		if (res.equals(blueSolution) || res.equals(redSolution)) {
			return null;
		}
		// else
		
		MovesAndSubFunctionInspectorFactory inspectorFactory = new MovesAndSubFunctionInspectorFactory() {
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
        };

		RBallEfficientHillClimberSnapshot solution = blue.getHillClimberForInstanceOf().initialize(res, inspectorFactory);
		lastRuntime = System.nanoTime()-initTime;
        
		return solution;
	}

}
