package neo.landscape.theory.apps.graphcoloring;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.Move;

public class NaiveHillClimber implements HillClimber<WeightedGraphColoring> {

	@Override
	public HillClimberForInstanceOf<WeightedGraphColoring> initialize(
			WeightedGraphColoring prob) {
		return new NaiveHillClimberForInstanceOf(this, prob);
	}

}
