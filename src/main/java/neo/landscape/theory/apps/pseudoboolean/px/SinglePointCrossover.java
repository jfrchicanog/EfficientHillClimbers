package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.Random;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class SinglePointCrossover implements CrossoverInternal {
	private EmbeddedLandscape el;
	private Random rnd = new Random();
	private PrintStream ps;
	private long lastRuntime;
	private VariableProcedence varProcedence;
	
	public SinglePointCrossover(EmbeddedLandscape el) {
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
		int randomNumber = rnd.nextInt(2*n);
		int position = (randomNumber >>> 1);
		boolean firstRed = ((randomNumber & 0x01)!=0);
		
		for (int i=0; i < n; i++) {
			if (red.getBit(i) == blue.getBit(i)) {
				varProcedence.markAsPurple(i);
			} else if ((firstRed && i >= position) || (!firstRed && i < position)) {
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
