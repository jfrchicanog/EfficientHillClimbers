package neo.landscape.theory.apps.pseudoboolean;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.Move;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class RBallPBMove implements Move<PseudoBooleanFunction, PBSolution> {

	public SetOfVars flipVariables;
	public double improvement;
	
	public RBallPBMove(double i, SetOfVars fv)
	{
		improvement = i;
		flipVariables =fv;
	}
	
	public RBallPBMove(double i, int n)
	{
		improvement = i;
		flipVariables = new SetOfVars();
	}
	
	public RBallPBMove() {
	}

	@Override
	public double getImprovement() {
		return improvement;
	}
	
	public String toString()
	{
		String str;
		str = flipVariables.toString()+" ("+improvement+")";
		return str;
	}

}
