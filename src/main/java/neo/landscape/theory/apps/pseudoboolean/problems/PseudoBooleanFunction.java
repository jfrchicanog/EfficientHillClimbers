package neo.landscape.theory.apps.pseudoboolean.problems;

import java.math.BigDecimal;
import java.util.Random;

import neo.landscape.theory.apps.efficienthc.Problem;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;

public abstract class PseudoBooleanFunction implements Problem {

	protected Random rnd;
	protected int n;
	protected long seed;

	public PseudoBooleanFunction() {
		super();
		rnd = new Random(seed);
	}

	@Override
	public void setSeed(long seed) {
		rnd = new Random(seed);
		this.seed = seed;
	}

	public long getSeed() {
		return seed;
	}

	public BigDecimal evaluateArbitraryPrecision(Solution sol) {
		return new BigDecimal(evaluate(sol));
	}

	@Override
	public PBSolution getRandomSolution() {
		PBSolution pbs = new PBSolution(n);

		for (int i = 0; i < n; i++) {
			pbs.setBit(i, rnd.nextInt(2));
		}

		return pbs;
	}

	public int getN() {
		return n;
	}

}