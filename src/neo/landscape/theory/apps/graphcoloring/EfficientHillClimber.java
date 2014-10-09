package neo.landscape.theory.apps.graphcoloring;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.efficienthc.Move;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.util.DoubleLinkedList;
import neo.landscape.theory.apps.util.DoubleLinkedList.Entry;

public class EfficientHillClimber implements HillClimber<WeightedGraphColoring> {

	private class EfficientHillClimberForInstanceOf implements HillClimberForInstanceOf<WeightedGraphColoring>
	{
		private WeightedGraphColoring prob;
		
		public EfficientHillClimberForInstanceOf(WeightedGraphColoring prob)
		{
			this.prob=prob;
		}
		
		@Override
		public HillClimberSnapshot<WeightedGraphColoring> initialize(
				Solution<? super WeightedGraphColoring> sol) {
			
			final Solution<? super WeightedGraphColoring> fsol=sol;
			
			return new HillClimberSnapshot<WeightedGraphColoring>() {

				private WGCSolution sol;
				
				private Entry<WGCMove> [][] s;
				private DoubleLinkedList<WGCMove> [] moves;
				private int maxImprovement;
				
				{
					if (fsol instanceof WGCSolution)
					{
						this.sol = (WGCSolution) sol;
					}
					else
					{
						throw new IllegalArgumentException("Expected an object of class WGCSOlution and got one of class "+sol.getClass().getCanonicalName());
					}
					// This can definitely be improved
					double val = prob.evaluate(sol);
					moves = new DoubleLinkedList [(int)val+1];
					computeSMatrix();
				}
				
				private void addToList(Entry<WGCMove> m)
				{
					int list = computeIndex(m.v.improvement);
					
					if (moves[list]==null)
					{
						moves[list] = new DoubleLinkedList<WGCMove>();
					}
					
					moves[list].add(m);
					
					if (list > maxImprovement)
					{
						maxImprovement = list;
					}
				}
				
				private void computeSMatrix()
				{
					int vertices= prob.getVertices();
					int colors = prob.getColors();
					int [][] adjacents = prob.getAdjacents();
					
					s = new Entry [prob.getVertices()][colors];
					
					double histogram []=null;
					maxImprovement=0;
					
					for (int u=0; u < vertices; u++)
					{
						histogram = new double [colors];
						
						for (int v: adjacents[u])
						{
							histogram[sol.colors[v]] += prob.getWeight(u, v);
						}
						
						for (int c=0; c < colors; c++)
						{
							Entry<WGCMove> m = new Entry(new WGCMove(u,c));
							m.v.improvement = histogram[c]-histogram[sol.colors[u]];
							s[u][c] = m;
							
							addToList(m);		
						}
					}
				}
				
				private void removeFromList(Entry<WGCMove> m)
				{
					int list = computeIndex(m.v.improvement);
					moves[list].remove(m);
				}
				
				private int computeIndex(double s)
				{
					int index= -(int)s;
					if (index < 0) index=0;
					return index;
				}
				
				private void updateSMatrix(WGCMove m)
				{
					int colors = prob.getColors();
					int [][] adjacents = prob.getAdjacents();
					
					double sud = m.improvement;
					
					// Update the values related to the m.vertex
					for (int c=0; c < colors; c++)
					{
						Entry<WGCMove> aux = s[m.vertex][c]; 
						removeFromList(aux);
						aux.v.improvement -= sud;
						addToList(aux);
						
					}
					
					int new_color = m.color;
					int old_color = sol.colors[m.vertex];
					
					// Update the adjacent nodes of m.vertex
					for (int t: adjacents[m.vertex])
					{
						if (sol.colors[t]==new_color)
						{
							// If it will be a conflict
							for (int c=0; c < colors; c++)
							{
								if (c != new_color && c != old_color)
								{
									Entry<WGCMove> aux = s[t][c];
									removeFromList(aux);
									aux.v.improvement -= prob.getWeight(m.vertex, t);
									addToList(aux);
								}
							}
						}
						else if (sol.colors[t] == old_color)
						{
							// If there was a conflict
							for (int c=0; c < colors; c++)
							{
								if (c != new_color && c != old_color)
								{
									Entry<WGCMove> aux = s[t][c];
									removeFromList(aux);
									aux.v.improvement += prob.getWeight(m.vertex, t);
									addToList(aux);
								}
							}
						}
						
						
						if (sol.colors[t]==new_color)
						{
							// There will be a conflict
							Entry<WGCMove> aux = s[t][old_color];
							removeFromList(aux);
							aux.v.improvement -= 2*prob.getWeight(m.vertex, t);
							addToList(aux);
						}
						
						else if (sol.colors[t] == old_color)
						{
							// There was a conflict
							Entry<WGCMove> aux = s[t][new_color];
							removeFromList(aux);
							aux.v.improvement += 2*prob.getWeight(m.vertex, t);
							addToList(aux);
						}
						else
						{
							Entry<WGCMove> aux = s[t][new_color];
							removeFromList(aux);
							aux.v.improvement += prob.getWeight(m.vertex, t);;
							addToList(aux);
							
							aux = s[t][old_color];
							removeFromList(aux);
							aux.v.improvement -= prob.getWeight(m.vertex, t);;
							addToList(aux);
							
						} 
					}
					
					checkMaxImprovement();
				}
				
				private void checkMaxImprovement()
				{
					while (moves[maxImprovement] == null || moves[maxImprovement].isEmpty())
					{
						maxImprovement--;
					}
				}

				
				
				/**
				 * Make a move and returns the decrease in fitness function (we minimize).
				 */
				@Override
				public double move() {
					
					if (maxImprovement == 0)
					{
						return maxImprovement;
					}
					else
					{
						// Select the movement
						
						WGCMove m = moves[maxImprovement].getFirst().v;
						double imp = m.improvement;
						
						// Update the S matrix
						
						updateSMatrix(m);
						
						// Do the movement
						
						sol.colors[m.vertex]=m.color;
						
						return imp;
					}
							
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
					WGCMove m = moves[maxImprovement].getFirst().v;
					WGCMove move = new WGCMove(m.vertex, m.color,-maxImprovement);
					return move;
				}



				@Override
				public HillClimberForInstanceOf<WeightedGraphColoring> getHillClimberForInstanceOf() {
					return EfficientHillClimberForInstanceOf.this;
				}
			};
		}

		@Override
		public HillClimber<WeightedGraphColoring> getHillClimber() {
			return EfficientHillClimber.this;
		}


		@Override
		public WeightedGraphColoring getProblem() {
			return prob;
		}	
	}

	
	@Override
	public HillClimberForInstanceOf<WeightedGraphColoring> initialize(
			WeightedGraphColoring prob) {
		
		return new EfficientHillClimberForInstanceOf(prob);
	
	}
	

}
