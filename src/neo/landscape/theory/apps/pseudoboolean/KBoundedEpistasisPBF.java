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
	
	@Override
	public double evaluate(Solution sol) {
		PBSolution pbs = (PBSolution)sol;
		
		double res = 0;
		// Build the subsolution
		
		PBSolution sub = new PBSolution(k);
		for (int i=0; i < m; i++)
		{	
			
			for (int j=0; j <k; j++)
			{
				sub.setBit(j,pbs.getBit(masks[i][j]));
			}
			// Evaluate
			res += evaluateSubfunction (i, sub);
		}
		
		return res;
	}

}
