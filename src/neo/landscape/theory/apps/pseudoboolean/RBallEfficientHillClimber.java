package neo.landscape.theory.apps.pseudoboolean;

import java.io.IOException;
import java.util.Arrays;
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
	public static class SetOfVars extends BitSet implements Iterable<Integer> {
		
		public int [] vars=null;
		
		public void inmute()
		{
			int [] aux = new int [cardinality()];
			int j=0;
			for (int v: this)
			{
				aux[j++] = v;
			}
			vars=aux;
		}
		
		public void add(int i)
		{
			if (vars == null)
			{
				set(i);
			}
			else
			{
				throw new RuntimeException("Cannot add more variables if the SetOfVars is inmmutable");
			}
			
		}
		
		public boolean contains(int i)
		{
			if (vars != null)
			{
				for (int j=0; j < vars.length; j++)
				{
					if (vars[j]==i)
					{
						return true;
					}
				}
				return false;
				
			}
			else
			{
				return get(i);
			}
		}
		
		public int size()
		{
			if (vars != null)
			{
				return vars.length;
			}
			else
			{
				return cardinality();
			}
		}
		
		
		
		@Override
		public int hashCode() {
			if (vars != null)
			{
				return Arrays.hashCode(vars);
			}
			else
			{
				return super.hashCode();
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (vars != null && obj instanceof SetOfVars && ((SetOfVars)obj).vars != null)
				return Arrays.equals(vars, ((SetOfVars)obj).vars);
			return super.equals(obj);
			
		}

		@Override
		public Iterator<Integer> iterator() {
			if (vars!=null)
			{
				return new Iterator<Integer>(){
					int ind=0;
					@Override
					public boolean hasNext() {
						return ind < vars.length;
					}

					@Override
					public Integer next() {
						return vars[ind++];
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			else
			{
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
	
	// Auxiliary data structures
	private int [] flip_bits;
	
	// Statistics of the operator
	private int [] moves_per_distance;
	private long problem_init_time;
	private long solution_init_time;


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
		
		moves_per_distance[minImpRadius]++;
		
		moveSeveralBitsEff(move.flipVariables);
		
		return imp;
	}

	@Override
	public PBSolution getSolution() {
		return sol;
	}
	
	private void moveSeveralBitsIneff(SetOfVars bits)
	{
		for (int var: bits)
		{
			moveOneBit(var);
		}
	}
	
	private void moveSeveralBitsEff(SetOfVars bits)
	{
		int [][] masks = problem.getMasks();
		int k= problem.getK();
		
		// Identify which which subfunctions will be afffected
		SetOfVars affected_subfns = new SetOfVars();
		for (int bit: bits)
		{
			for (int sf: problem.getAppearsIn()[bit])
			{
				affected_subfns.add(sf);
			}
		}
		
		for (int sf: affected_subfns)
		{
			// For each subfunction do ...			
			// For each move score evaluate the subfunction and update the corresponding value
			PBSolution sub = new PBSolution(k);
			for (int j=0; j < k; j++)
			{
				int bit = masks[sf][j];
				sub.setBit(j,sol.getBit(bit));
			}
			double v_sub = problem.evaluateSubfunction(sf, sub); 
			
			for (int e_ind: subfns[sf])
			{
				Entry<RBallPBMove> e = mos[e_ind];
				SetOfVars sov = e.v.flipVariables;
				
				// Build the subsolutions
				PBSolution sub_sov = new PBSolution(sub);
				
				int ind_i = 0;
				for (int j=0; j < k; j++)
				{
					int bit = masks[sf][j];
					if (sov.contains(bit))
					{
						sub_sov.flipBit(j);
					}
					
					if (bits.contains(bit))
					{
						flip_bits[ind_i++]=j; 
					}
				}
				
				double v_sub_sov = problem.evaluateSubfunction(sf, sub_sov); 
				
				for (int j=0; j < ind_i; j++)
				{
					sub.flipBit(flip_bits[j]);
					sub_sov.flipBit(flip_bits[j]);
				}
				
				double v_sub_sov_i = problem.evaluateSubfunction(sf, sub_sov);
				double v_sub_i = problem.evaluateSubfunction(sf, sub);
				
				for (int j=0; j < ind_i; j++)
				{
					sub.flipBit(flip_bits[j]);
				}
				
				double update = (v_sub_sov_i - v_sub_i) - (v_sub_sov-v_sub); 
				
				double old = e.v.improvement;
				e.v.improvement += update;
				if ( (old > 0 && old+update > 0) || (old <= 0 && old+update <= 0) )
				{
					// nothing to do
				}
				else if (old > 0)
				{
					int p = sov.size();
					improving[p].remove(e);
					nonImproving[p].add(e);
				}
				else
				{
					int p = sov.size();
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
		for (int bit: bits)
		{
			sol.flipBit(bit);
		}
	}
	
	public void moveOneBit(int i)
	{
		int [][] masks = problem.getMasks();
		int k= problem.getK();
		
		// Identify which in which functions does i appears (appearsIn array)
		for (int sf: problem.getAppearsIn()[i])
		{
			// For each subfunction do ...			
			// For each move score evaluate the subfunction and update the corresponding value
			
			PBSolution sub = new PBSolution(k);
			for (int j=0; j < k; j++)
			{
				int bit = masks[sf][j];
				sub.setBit(j,sol.getBit(bit));
			}
			double v_sub = problem.evaluateSubfunction(sf, sub); 
			
			for (int e_ind: subfns[sf])
			{
				Entry<RBallPBMove> e = mos[e_ind];
				SetOfVars sov = e.v.flipVariables;
				
				// Build the subsolutions
				
				PBSolution sub_sov = new PBSolution(sub);
				int ind_i =0;
				for (int j=0; j < k; j++)
				{
					int bit = masks[sf][j];
					if (sov.contains(bit))
					{
						sub_sov.flipBit(j);
					}
					
					if (bit==i)
					{
						ind_i=j;  // Earlier to save time
					}
				}
				
				double v_sub_sov = problem.evaluateSubfunction(sf, sub_sov);
				
				sub.flipBit(ind_i);
				sub_sov.flipBit(ind_i);
				double v_sub_sov_i = problem.evaluateSubfunction(sf, sub_sov);
				double v_sub_i = problem.evaluateSubfunction(sf, sub);
				
				sub.flipBit(ind_i);
				
				double update = (v_sub_sov_i - v_sub_i) - (v_sub_sov-v_sub); 
				
				double old = e.v.improvement;
				e.v.improvement += update;
				if ( (old > 0 && old+update > 0) || (old <= 0 && old+update <= 0) )
				{
					// nothing to do
				}
				else if (old > 0)
				{
					int p = sov.size();
					improving[p].remove(e);
					nonImproving[p].add(e);
				}
				else
				{
					int p = sov.size();
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
		long init=System.currentTimeMillis();
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
			nonImproving[sov.size()].add(e);
		}
		
		problem_init_time = System.currentTimeMillis()-init;
	}
	
	private void initializeSolutionDependentStructures()
	{
		long init = System.currentTimeMillis();
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
				int p = sov.size();
				nonImproving[p].remove(e);
				improving[p].add(e);
				
				if (p < minImpRadius)
				{
					minImpRadius=p;
				}
			}
		}
		
		solution_init_time = System.currentTimeMillis()-init;
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
		moves_per_distance = new int [radius+1];
		flip_bits = new int [radius];
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
								SetOfVars sov = new SetOfVars();
								for (int v=1; v <= p; v++)
								{
									sov.add(variables[v]);
								}
								sov.inmute();
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
				assert move.flipVariables.size() == p;
				assert move.improvement > 0;
			}
			
			for (RBallPBMove move: nonImproving[p])
			{
				assert move.flipVariables.size() == p;
				assert move.improvement <= 0;
			}
		}
	}
	
	public int [] getMovesPerDinstance()
	{
		return moves_per_distance;
	}
	
	public int getStoredScores()
	{
		return mos.length;
	}
	
	public long getProblemInitTime() {
		return problem_init_time;
	}


	public long getSolutionInitTime() {
		return solution_init_time;
	}
	

}
