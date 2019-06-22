package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public interface CrossoverInternal {
	public EmbeddedLandscape getEmbddedLandscape();
	public VariableProcedence getVarProcedence();
	public PBSolution recombineInternal(PBSolution blue, PBSolution red);
	public void setPrintStream(PrintStream ps);
	public void setSeed(long seed);
}