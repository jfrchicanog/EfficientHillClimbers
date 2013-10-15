package neo.landscape.theory.apps.pseudoboolean;

import java.util.Random;

import neo.landscape.theory.apps.efficienthc.Problem;
import neo.landscape.theory.apps.efficienthc.Solution;

/**
 * This class represents a K-bounded epistasis Pseudo-Boolean Function. It contains m subfunctions with k bits each.
 * This class is an abstraction for NK-landscapes and MAX-k-SAT
 * @author francis
 *
 */

public abstract class KBoundedEpistasisPBF extends AdditivelyDecomposablePBF implements Problem {
	
	protected int k; // epistasis degree
	
	public KBoundedEpistasisPBF()
	{
		super();
	}

	public int getK()
	{
		return k;
	}

}
