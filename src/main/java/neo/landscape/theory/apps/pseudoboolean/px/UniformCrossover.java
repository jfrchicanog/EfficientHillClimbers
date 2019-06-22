package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class UniformCrossover implements CrossoverInternal {
	private EmbeddedLandscape el;
	private Random rnd = new Random();
	private PrintStream ps;
	private long lastRuntime;
	private VariableProcedence varProcedence;
	
	public UniformCrossover(EmbeddedLandscape el) {
		this.el=el;
		varProcedence = new ComponentAndVariableMask(el.getN());
	}

	@Override
	public EmbeddedLandscape getEmbddedLandscape() {
		return el;
	}

	@Override
	public VariableProcedence getVarProcedence() {
		return varProcedence;
	}

	@Override
	public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
		long initTime = System.nanoTime();
		PBSolution child = new PBSolution(red); //child, copy of red
		int n = el.getN();
		for (int i=0; i < n; i++) {
			if (red.getBit(i) == blue.getBit(i)) {
				varProcedence.markAsPurple(i);
			} else if (rnd.nextBoolean()) {
				child.flipBit(i);
				varProcedence.markAsBlue(i);
			} else {
				varProcedence.markAsRed(i);
			}
		}

		lastRuntime = System.nanoTime() - initTime;
		return child;
	}
	
	public PBSolution recombine(PBSolution blue, PBSolution red) {
		PBSolution solution = recombineInternal(blue, red);
		if (ps != null) {
	    	ps.println("Recombination time:"+lastRuntime);
	    }
		return solution;
	}

	@Override
	public void setPrintStream(PrintStream ps) {
		this.ps=ps;
	}

	@Override
	public void setSeed(long seed) {
		rnd = new Random(seed);
	}

}
