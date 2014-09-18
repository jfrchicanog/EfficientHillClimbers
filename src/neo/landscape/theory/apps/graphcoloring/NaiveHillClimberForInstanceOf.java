package neo.landscape.theory.apps.graphcoloring;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.efficienthc.Solution;

public class NaiveHillClimberForInstanceOf implements HillClimberForInstanceOf<WeightedGraphColoring>
{
	/**
	 * 
	 */
	private final NaiveHillClimber naiveHillClimber;
	WeightedGraphColoring prob;
	
	public NaiveHillClimberForInstanceOf(NaiveHillClimber naiveHillClimber, WeightedGraphColoring prob)
	{
		this.naiveHillClimber = naiveHillClimber;
		this.prob=prob;
	}
	
	/*
	public NaiveHillClimberSnapshot initialize(Solution<? super WeightedGraphColoring> sol) {
		return new NaiveHillClimberSnapshot(this, sol);
	}*/

	@Override
	public NaiveHillClimber getHillClimber() {
		return this.naiveHillClimber;
	}

	@Override
	public NaiveHillClimberSnapshot initialize(
			Solution<? super WeightedGraphColoring> sol) {
		return new NaiveHillClimberSnapshot(this, sol);
	}


	@Override
	public WeightedGraphColoring getProblem() {
		return prob;
	}
	
}