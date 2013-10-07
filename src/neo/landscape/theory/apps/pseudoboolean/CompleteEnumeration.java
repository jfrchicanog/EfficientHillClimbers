package neo.landscape.theory.apps.pseudoboolean;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;

public class CompleteEnumeration<P extends PseudoBooleanFunction> implements ExactSolutionMethod<PseudoBooleanFunction> {

	@Override
	public SolutionQuality<PseudoBooleanFunction> solveProblem(PseudoBooleanFunction problem) {
		
		int n=problem.getN();
		PBSolution sol = new PBSolution(n);
		int [] data = sol.getData();
		SolutionQuality<PseudoBooleanFunction> s = new SolutionQuality<PseudoBooleanFunction>();
		
		if (n >= 31)
		{
			throw new RuntimeException("A long search of "+n+ " bits. I will not do that!");
		}
		
		s.quality=-Double.MAX_VALUE;
		
		int limit = 1 << n;
		for (data[0]=0;data[0] < limit; data[0]++)
		{
			double val = problem.evaluate(sol);
			if (val > s.quality)
			{
				s.quality = val;
				s.solution = new PBSolution(sol);
			}
		}
		return s;
	}

	

}
