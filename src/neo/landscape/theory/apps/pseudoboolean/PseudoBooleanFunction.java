package neo.landscape.theory.apps.pseudoboolean;

import java.util.Random;

import neo.landscape.theory.apps.efficienthc.Problem;

public abstract class PseudoBooleanFunction implements Problem {

	protected Random rnd;
	protected int n;

	public PseudoBooleanFunction() {
		super();
		rnd = new Random();
	}

	@Override
	public void setSeed(long seed) {
		rnd = new Random (seed);
	}

	@Override
	public PBSolution getRandomSolution() {
		PBSolution pbs = new PBSolution (n);
		
		for (int i=0; i < n; i++)
		{
			pbs.setBit(i, rnd.nextInt(2));
		}
	
		return pbs;
	}

	public int getN() {
		return n;
	}

}