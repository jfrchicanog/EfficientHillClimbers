package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;

public interface Crossover {
	RBallEfficientHillClimberSnapshot recombine(RBallEfficientHillClimberSnapshot blue, RBallEfficientHillClimberSnapshot red);
	void setPrintStream(PrintStream ps);
	void setSeed(long seed);

}