package neo.landscape.theory.apps.graphcoloring;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.Solution;

public class NaiveHillClimber implements HillClimber<WeightedGraphColoring> {

	private WeightedGraphColoring prob;
	private WGCSolution sol;
	
	private WGCMove movement;
	
	
	@Override
	public void initialize(WeightedGraphColoring prob, Solution<WeightedGraphColoring> sol) {
		this.prob = prob;
		movement=null;
		if (sol instanceof WGCSolution)
		{
			this.sol = (WGCSolution) sol;
		}
		else
		{
			throw new IllegalArgumentException("Expected an object of class WGCSOlution and got one of class "+sol.getClass().getCanonicalName());
		}
	}
	
	/**
	 * Make a move and returns the decrease in fitness function (we minimize).
	 */
	@Override
	public double move() {
		
		getMovement();
				
		// Do the movement (if it is an improvement move)
		double imp = movement.improvement;
		if (imp < 0)
		{
			sol.colors[movement.vertex] = movement.color;
			movement=null;
		}
		
		return imp;
		
	}
	


	/**
	 * Returns the solution (not a copy)
	 */
	@Override
	public Solution<WeightedGraphColoring> getSolution() {
		return sol;
	}

	@Override
	public WGCMove getMovement() {
		
		if (movement != null)
		{
			return movement;
		}
		
		// else
		
		movement = new WGCMove(0,sol.colors[0],0);
		
		int colors = prob.getColors();
		int [][] adjacents = prob.getAdjacents();
		
		double histogram [];
		
		for (int u=0; u < prob.getVertices(); u++)
		{
			histogram = new double [colors];
			for (int v: adjacents[u])
			{
				histogram[sol.colors[v]] += prob.getWeight(u,v);
			}
			
			int min=0;
			for (int i=1; i < colors; i++)
			{
				if (histogram[i] < histogram[min])
				{
					min=i;
				}
			}
			
			if (histogram[min] - histogram[sol.colors[u]] < movement.improvement)
			{
				movement.improvement = histogram[min] - histogram[sol.colors[u]];
				movement.vertex = u;
				movement.color = min;
			}
				
		}

		return movement;
	}

}
