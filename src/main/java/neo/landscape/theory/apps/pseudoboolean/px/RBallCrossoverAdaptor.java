package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionInspectorFactory;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class RBallCrossoverAdaptor implements RBallCrossover  {
	
	private MovesAndSubFunctionInspectorFactory inspectorFactory;
	private CrossoverInternal crossover;
	private PrintStream ps;

	public RBallCrossoverAdaptor(CrossoverInternal crossover) {
		this.crossover = crossover;
	}

	public RBallEfficientHillClimberSnapshot recombine(
			RBallEfficientHillClimberSnapshot blue,
			RBallEfficientHillClimberSnapshot red) {
	    
	    long initTime = System.nanoTime();
	    
		PBSolution blueSolution = blue.getSolution();
		PBSolution redSolution = red.getSolution();
		PBSolution res = crossover.recombineInternal(blueSolution, redSolution);
		
		long lastRuntime = System.nanoTime()-initTime;

		if (res.equals(blueSolution) || res.equals(redSolution)) {
			reportRuntime(lastRuntime);
			return null;
		}
		// else
		
		inspectorFactory = new InitializedMovesAndSubFunctionInspectorFactory(blue, red, crossover);
		RBallEfficientHillClimberSnapshot solution = blue.getHillClimberForInstanceOf().initialize(res, inspectorFactory);
		
		lastRuntime = System.nanoTime()-initTime;
		reportRuntime(lastRuntime);
		
		return solution;
	}

	protected void reportRuntime(long lastRuntime) {
		if (ps != null) {
			ps.println("Recombination time:"+lastRuntime);
		}
	}
	

	@Override
	public void setSeed(long seed) {
		crossover.setSeed(seed);
	}

	@Override
	public void setPrintStream(PrintStream ps) {
		this.ps=ps;
		crossover.setPrintStream(ps);
	}
	
	

}
