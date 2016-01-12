package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

import neo.landscape.theory.apps.efficienthc.mo.MultiobjectiveMove;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;

public class VectorPBMove implements MultiobjectiveMove<VectorMKLandscape, PBSolution>{

	public SetOfVars flipVariables;
	public double [] deltas;

	public VectorPBMove(double [] deltas, SetOfVars fv) {
		this.deltas = deltas;
		flipVariables = fv;
	}

	public VectorPBMove(double [] deltas, int n) {
		this.deltas = deltas;
		flipVariables = new SetOfVars();
	}

	public VectorPBMove() {
	}

	public String toString() {
		String str;
		str = flipVariables.toString() + " (" + deltas + ")";
		return str;
	}

    @Override
    public double[] getImprovement() {
        return deltas;
    }

}
