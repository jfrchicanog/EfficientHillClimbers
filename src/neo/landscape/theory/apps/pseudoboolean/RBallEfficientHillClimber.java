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
import neo.landscape.theory.apps.util.IteratorFromArray;
import neo.landscape.theory.apps.util.RootedTreeGenerator;
import neo.landscape.theory.apps.util.RootedTreeGenerator.RootedTreeCallback;

public class RBallEfficientHillClimber implements HillClimber<KBoundedEpistasisPBF> {

	public static class SetOfSetOfVars extends HashSet<SetOfVars>{}
	public static class SetOfVars extends BitSet implements Iterable<Integer> {
		
		public int [] vars=null;
		
		protected SetOfVars (int [] v)
		{
			vars =v;
		}
		
		public static SetOfVars immutable(int ... v)
		{
			return new SetOfVars(v);
		}
		
		public SetOfVars()
		{
			super();
		}
		
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
				return IteratorFromArray.iterator(vars);
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
	private PBSolution sub;
	private PBSolution sub_sov;
	private PBSolution sol_sov;
	private SetOfVars affected_subfns;
	
	// Statistics of the operator
	private int [] moves_per_distance;
	private long problem_init_time;
	private long solution_init_time;
	private long solution_init_evals;
	private long solution_move_evals;
	private long total_moves;
	private long total_sol_inits;


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
			if (false&&this.sol != null)
			{
				initializeSolutionDependentStructuresFromSolution((PBSolution) sol);
			}
			else
			{
				this.sol = (PBSolution) sol;
				initializeSolutionDependentStructuresFromScratch();
			}
		}
		else
		{
			throw new IllegalArgumentException ("Expected argument of class PBSolution but found "+sol.getClass().getCanonicalName());
		}

	}
	
	private void initializeSolutionDependentStructuresFromSolution(PBSolution to_this)
	{
		long init = System.currentTimeMillis();
		// Move by doing partial updates
		//SetOfVars aux = new SetOfVars();
		int n= problem.getN();
		for (int v=0; v < n; v++)
		{
			if (to_this.getBit(v) != this.sol.getBit(v))
			{
				moveOneBit(v);
			}
		}
		solution_init_time = System.currentTimeMillis()-init;
		total_sol_inits++;
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
	
	private Iterable<Integer> subFunctionsAffected(SetOfVars bits)
	{
		// Identify which which subfunctions will be afffected
		Iterable<Integer> sfs;
		
		if (bits.size()==1)
		{
			int bit = bits.iterator().next();
			sfs = IteratorFromArray.iterable(problem.getAppearsIn()[bit]);
		}
		else
		{
			affected_subfns.clear();
			for (int bit: bits)
			{
				for (int sf: problem.getAppearsIn()[bit])
				{
					affected_subfns.add(sf);
				}
			}
			sfs = affected_subfns;
		}
		return sfs;
	}
	
	private void moveSeveralBitsEff(SetOfVars bits)
	{
		int [][] masks = problem.getMasks();
		int k= problem.getK();
		
		// Identify which which subfunctions will be afffected
		
		for (int sf: subFunctionsAffected(bits))
		{
			// For each subfunction do ...			
			// For each move score evaluate the subfunction and update the corresponding value
			for (int j=0; j < k; j++)
			{
				int bit = masks[sf][j];
				sub.setBit(j,sol.getBit(bit));
			}
			double v_sub = problem.evaluateSubfunction(sf, sub);
			
			solution_move_evals++;
			
			for (int e_ind: subfns[sf])
			{
				Entry<RBallPBMove> e = mos[e_ind];
				SetOfVars sov = e.v.flipVariables;
				
				// Build the subsolutions
				sub_sov.copyFrom(sub);
				
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
				
				solution_move_evals++;
				
				for (int j=0; j < ind_i; j++)
				{
					sub.flipBit(flip_bits[j]);
					sub_sov.flipBit(flip_bits[j]);
				}
				
				double v_sub_sov_i = problem.evaluateSubfunction(sf, sub_sov);
				double v_sub_i = problem.evaluateSubfunction(sf, sub);
				
				solution_move_evals += 2;
				
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
		
		total_moves++;
	}
	
	public void moveOneBit(int i)
	{
		moveSeveralBitsEff(SetOfVars.immutable(i));
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
		
		sub = new PBSolution (problem.getK());
		sub_sov = new PBSolution (problem.getK());
		sol_sov = new PBSolution (problem.getN());
		affected_subfns = new SetOfVars();
		sol = null;
		
		
		problem_init_time = System.currentTimeMillis()-init;
	}
	
	private void initializeSolutionDependentStructuresFromScratch()
	{
		long init = System.currentTimeMillis();
		// Compute the scores for all the values of the map
		int k = problem.getK();
		int [][] masks = problem.getMasks();
		minImpRadius = radius+1;
		
		double update ;
		
		for (Entry<RBallPBMove> e : mos)
		{
			SetOfVars sov = e.v.flipVariables;
			
			update = 0;
			
			for (int sf: subFunctionsAffected(sov))
			{
				// For each subfunction do ...			
				for (int j=0; j < k; j++)
				{
					int bit = masks[sf][j];
					sub.setBit(j,sol.getBit(bit));
				}
				double v_sub = problem.evaluateSubfunction(sf, sub); 

				// Build the subsolutions
				sub_sov.copyFrom(sub);

				for (int j=0; j < k; j++)
				{
					int bit = masks[sf][j];
					if (sov.contains(bit))
					{
						sub_sov.flipBit(j);
					}
				}

				double v_sub_sov = problem.evaluateSubfunction(sf, sub_sov); 
				
				solution_init_evals += 2;
					
				update += v_sub_sov - v_sub;
			}
			double old_value = e.v.improvement;
			e.v.improvement = update;
			int p = sov.size();
			
			if ( (old_value > 0 && update > 0) || (old_value <= 0 && update <= 0) )
			{
				// nothing to do
			}
			else if (old_value > 0)
			{
				improving[p].remove(e);
				nonImproving[p].add(e);
			}
			else
			{
				nonImproving[p].remove(e);
				improving[p].add(e);
				
				if (p < minImpRadius)
				{
					minImpRadius = p;
				}
				
			}
		}
		
		total_sol_inits++;
		
		solution_init_time = System.currentTimeMillis()-init;
		
		//System.out.println(solution_init_time);
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
		
		solution_init_evals=0;
		solution_move_evals=0;
		total_moves = 0;
		total_sol_inits = 0;
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
	
	public long getTotalMoves()
	{
		return total_moves;
	}
	
	public long getTotalSolutionInits()
	{
		return total_sol_inits;
	}
	
	public long getSubfnsEvalsInMoves()
	{
		return solution_move_evals;
	}
	
	public long getSubfnsEvalsInSolInits()
	{
		return solution_init_evals;
	}
	

}
