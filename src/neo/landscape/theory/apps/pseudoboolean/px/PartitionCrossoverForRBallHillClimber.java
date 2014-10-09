package neo.landscape.theory.apps.pseudoboolean.px;

import neo.landscape.theory.apps.pseudoboolean.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.RBallEfficientHillClimberSnapshot;

public class PartitionCrossoverForRBallHillClimber extends PartitionCrossover {

	public PartitionCrossoverForRBallHillClimber(EmbeddedLandscape el) {
		super(el);
	}
	
	
	public RBallEfficientHillClimberSnapshot recombine (RBallEfficientHillClimberSnapshot blue, RBallEfficientHillClimberSnapshot red)
	{
		PBSolution blueSolution = blue.getSolution();
		PBSolution redSolution = red.getSolution();
		PBSolution res = recombine(blueSolution, redSolution);
		
		if (res.equals(blueSolution) || res.equals(redSolution))
		{
			return null;
		}
		// else
		
		return blue.getHillClimberForInstanceOf().initialize(res);
	}

	
}
