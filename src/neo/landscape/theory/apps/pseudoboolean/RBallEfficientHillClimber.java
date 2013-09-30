package neo.landscape.theory.apps.pseudoboolean;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.util.DoubleLinkedList;
import neo.landscape.theory.apps.util.DoubleLinkedList.Entry;
import neo.landscape.theory.apps.util.RootedTreeGenerator;
import neo.landscape.theory.apps.util.RootedTreeGenerator.RootedTreeCallback;

public class RBallEfficientHillClimber implements HillClimber<KBoundedEpistasisPBF> {

	public static class SetOfSetOfVars extends HashSet<SetOfVars>{}
	public static class SetOfVars extends BitSet implements Iterable<Integer>{
		public SetOfVars(int n)
		{
			super(n);
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>(){
				int var=nextSetBit(0);

				@Override
				public boolean hasNext() {
					return var!=-1;
				}

				@Override
				public Integer next() {
					int res=var;
					if (res < 0)
					{
						throw new NoSuchElementException();
					}
					else
					{
						var=nextSetBit(var+1);
					}
					
					return res;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}
		
	}
	
	private interface RecursiveCalls {
		public void run(int depth);
	}

	private KBoundedEpistasisPBF problem;
	private PBSolution sol;
	
	private DoubleLinkedList<RBallPBMove> [] improving;
	private DoubleLinkedList<RBallPBMove> [] nonImproving;
	private int minImpRadius;
	private Entry<RBallPBMove> [] mos;
	private int [][] subfns;
	
	private int radius;
	
	public RBallEfficientHillClimber(int r)
	{
		this.radius=r;
		initializeOperatorDependentStructures();
	}


	@Override
	public void initialize(KBoundedEpistasisPBF prob, Solution<KBoundedEpistasisPBF> sol) {
		if (prob != problem)
		{
			problem = prob;
			initializeProblemDependentStructures();
		}
		
		if (sol instanceof PBSolution)
		{
			this.sol = (PBSolution) sol;
			initializeSolutionDependentStructures();
		}
		else
		{
			throw new IllegalArgumentException ("Expected argument of class PBSolution but found "+sol.getClass().getCanonicalName());
		}

	}

	@Override
	public RBallPBMove getMovement() {
		if (minImpRadius > radius)
		{
			return new RBallPBMove(0, problem.getN());
		}
		else
		{
			return improving[minImpRadius].getFirst().v;
		}
	}

	@Override
	public double move() {
		if (minImpRadius > radius)
		{
			return 0;
		}
		
		// else
		
		RBallPBMove move = improving[minImpRadius].getFirst().v;
		double imp = move.improvement;
		
		
		// Roll out over 1 bit update (updating also the solution)
		
		for (int var: move.flipVariables)
		{
			moveOneBit(var);
		}
		
		return imp;
	}

	@Override
	public PBSolution getSolution() {
		return sol;
	}
	
	private void moveOneBit(int i)
	{
		int [][] masks = problem.getMasks();
		
		// Identify which in which functions does i appears (appearsIn array)
		for (int sf: problem.getAppearsIn()[i])
		{
			// For each subfunction do ...			
			// For each move score evaluate the subfunction and update the corresponding value
			for (int e_ind: subfns[sf])
			{
				Entry<RBallPBMove> e = mos[e_ind];
				SetOfVars sov = e.v.flipVariables;
				
				// Build the subsolutions
				int k= problem.getK();
				PBSolution sub_sov = new PBSolution(k);
				PBSolution sub = new PBSolution(k);
				
				int ind_i =0;
				for (int j=0; j < k; j++)
				{
					int bit = masks[sf][j];
					sub.setBit(j,sol.getBit(bit));
					sub_sov.setBit(j,sol.getBit(bit) ^ (sov.get(bit)?0x01:0x00));
					
					if (bit==i)
					{
						ind_i=j;  // Earlier to save time
					}
				}
				
				double v_sub_sov = problem.evaluateSubfunction(sf, sub_sov); // Probably can be computed earlier to save time
				double v_sub = problem.evaluateSubfunction(sf, sub);
				
				sub.flipBit(ind_i);
				sub_sov.flipBit(ind_i);
				double v_sub_sov_i = problem.evaluateSubfunction(sf, sub_sov);
				double v_sub_i = problem.evaluateSubfunction(sf, sub);
				
				double update = (v_sub_sov_i - v_sub_i) - (v_sub_sov-v_sub); 
				
				double old = e.v.improvement;
				e.v.improvement += update;
				if ( (old > 0 && old+update > 0) || (old <= 0 && old+update <= 0) )
				{
					// nothing to do
				}
				else if (old > 0)
				{
					int p = sov.cardinality();
					improving[p].remove(e);
					nonImproving[p].add(e);
				}
				else
				{
					int p = sov.cardinality();
					nonImproving[p].remove(e);
					improving[p].add(e);
					
					if (p < minImpRadius)
					{
						minImpRadius = p;
					}
					
				}
				
			}
		}
		
		// Update the minImpRadius variable
		while (minImpRadius <= radius && improving[minImpRadius].isEmpty())
		{
			minImpRadius++;
		}
		
		// Finally, flip the bit in the solution and we are done
		sol.flipBit(i);
	}
	
	private void initializeProblemDependentStructures()
	{
		// This map is to implement a one-to-one function between SetOfVars and integers (Minimal Perfect Hashing Function)
		Map<SetOfVars,Integer> aux_map = new HashMap<SetOfVars,Integer>();
		subfns = new int [problem.getM()][];
		
		int index=0;
		for (int sf=0; sf < subfns.length; sf++)
		{
			SetOfSetOfVars ssv = generateTuples(problem.getMasks()[sf]); 
			
			subfns[sf] = new int [ssv.size()];
			int j=0;
			for (SetOfVars sov: ssv)
			{
				Integer val = aux_map.get(sov);
				if (val == null)
				{
					val = index;
					aux_map.put(sov, val);
					index++;
				}
				subfns[sf][j++]=val;
			}
		}
		
		mos = new Entry [aux_map.size()];
		
		for (Map.Entry<SetOfVars, Integer> entry : aux_map.entrySet())
		{
			SetOfVars sov = entry.getKey();
			RBallPBMove rmove = new RBallPBMove(0, sov);
			Entry<RBallPBMove> e = new Entry<RBallPBMove>(rmove);
			mos[entry.getValue()] = e;
			nonImproving[sov.cardinality()].add(e);
		}
	}
	
	private void initializeSolutionDependentStructures()
	{
		// Compute the scores for all the values of the map
		double v_sol = problem.evaluate(sol);
		minImpRadius = radius+1;
		
		for (Entry<RBallPBMove> e : mos)
		{
			SetOfVars sov = e.v.flipVariables;
			
			PBSolution sol_sov = new PBSolution (sol);
			for (int bit: sov)
			{
				sol_sov.flipBit(bit);
			}
			
			double v_sol_sov = problem.evaluate(sol_sov);
			e.v.improvement = v_sol_sov - v_sol;
			
			if (e.v.improvement > 0)
			{
				int p = sov.cardinality();
				nonImproving[p].remove(e);
				improving[p].add(e);
				
				if (p < minImpRadius)
				{
					minImpRadius=p;
				}
			}
		}
	}
	
	private void initializeOperatorDependentStructures() {
		improving = new DoubleLinkedList [radius+1];
		nonImproving = new DoubleLinkedList[radius+1];
		
		for (int i=1; i <= radius; i++)
		{
			improving[i] = new DoubleLinkedList<RBallPBMove>();
			nonImproving[i] = new DoubleLinkedList<RBallPBMove>();
		}
		
		minImpRadius = radius+1;
	}
	
	private SetOfSetOfVars generateTuples(int [] init_vars)
	{
		if (init_vars ==null)
		{
			init_vars = new int [problem.getN()];
			for (int i=0; i < init_vars.length; i++)
			{
				init_vars[i]=i;
			}
		}
		
		final int [] init_vars_f = init_vars;
		final SetOfSetOfVars ssv = new SetOfSetOfVars();
		RootedTreeGenerator rtg = new RootedTreeGenerator();
		
		// Generate all the trees up to r and for each tree do
		rtg.generate(radius, new RootedTreeCallback() {
			int [] aux_siblings= new int [radius+1];
			int [] variables = new int[radius+1];
			int [] indices = new int [radius+1];
			int [] siblings= new int [radius+1];
			int [][] interactions = problem.getInteractions();
			@Override
			public void rootedTree(final int[] par, final int p) {
				// Navigate over the variables of the subfunction and plug-in the variables in the tree

				// First, analyze the tree, computing the siblings
				aux_siblings[0]=0;
				for (int i=1; i <= p; i++)
				{
					siblings[i] = aux_siblings[par[i]];
					aux_siblings[par[i]] = i;
					aux_siblings[i]=0;
				}
				// Now, plug the variables in the tree (array "variables")
				
				variables[0] = -1;
				
				new RecursiveCalls(){
					@Override
					public void run(int depth) {
						int par_var;
						int [] src;

						if (siblings[depth]!=0)
						{
							// To avoid permutations over sets
							indices[depth] = indices[siblings[depth]]+1;
						}
						else
						{
							indices[depth] = 0;
						}
						
						indices[depth] = 0;

						par_var = variables[par[depth]];
						src = (par_var < 0)?init_vars_f:interactions[par_var];

						for (;indices[depth]<src.length; indices[depth]++)
						{
							variables[depth] = src[indices[depth]];
							// Check if the variable appeared before
							int ptr=1;
							while (ptr < depth && variables[ptr]!=variables[depth])
							{
								ptr++;
							}

							if (ptr < depth)
							{
								// The variable appeared, we have to continue with the next variable
								continue;
							}
							// else the variable didn't appear so we follow in the body 
							// Check if we assigned all the variables
							if (depth == p)
							{
								// If this is the case, we have to mark the set of variables as "to be updated"
								// For each combination, mark the sets of variables as "touched"
								SetOfVars sov = new SetOfVars(p);
								for (int v=1; v <= p; v++)
								{
									sov.set(variables[v]);
								}
								ssv.add(sov);
							}
							else
							{
								run (depth+1);
							}
						}
					}}.run(1);
			}}); 
		return ssv;
	}
	
	public void checkConsistency()
	{
		for (Entry<RBallPBMove> e: mos)
		{
			SetOfVars sov = e.v.flipVariables;
			PBSolution sol_sov = new PBSolution (sol);
			
			for (int var: sov)
			{
				sol_sov.flipBit(var);
			}

			// Check the values of the scores
			double diff = problem.evaluate(sol_sov) - problem.evaluate(sol);
			assert e.v.improvement == diff : new RuntimeException("Expected "+ diff+" found "+e.v.improvement+ " in "+sov);
			
		}
		// Check if they are in the correct list
		for (int p=1; p <= radius; p++)
		{
			for (RBallPBMove move: improving[p])
			{
				assert move.flipVariables.cardinality() == p;
				assert move.improvement > 0;
			}
			
			for (RBallPBMove move: nonImproving[p])
			{
				assert move.flipVariables.cardinality() == p;
				assert move.improvement <= 0;
			}
		}
	}
	

}
