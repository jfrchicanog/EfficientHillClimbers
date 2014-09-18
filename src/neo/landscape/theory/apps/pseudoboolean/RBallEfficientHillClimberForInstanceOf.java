package neo.landscape.theory.apps.pseudoboolean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.HillClimber;
import neo.landscape.theory.apps.efficienthc.HillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.HillClimberSnapshot;
import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfSetOfVars;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.util.DoubleLinkedList;
import neo.landscape.theory.apps.util.DoubleLinkedList.Entry;
import neo.landscape.theory.apps.util.IteratorFromArray;
import neo.landscape.theory.apps.util.RootedTreeGenerator;
import neo.landscape.theory.apps.util.RootedTreeGenerator.RootedTreeCallback;
import neo.landscape.theory.apps.util.Seeds;

public class RBallEfficientHillClimberForInstanceOf implements HillClimberForInstanceOf<EmbeddedLandscape> {
	
	
	private interface RecursiveCalls {
		public void run(int depth);
	}

	/* Operator dependent structures */
	protected RBallEfficientHillClimber rball;
	
		
	/* Problem dependent structures */
	
	/* Problem info */
	protected EmbeddedLandscape problem;
	// Main configuration parameters and variables
	
	/* Problem info */
	protected int [][] subfns;
	/* Problem info */
	protected int [] oneFlipScores;
	// Auxiliary data structures
	/* Problem info */
	private int [] variables;
	private int [] indices;
	// Statistics of the operator
	/* Problem info */
	private long problem_init_time;
	/* Problem info */
	protected Properties configuration;
	
	/* This is required to build structures for several solutions */
	/* Probably we can reduce the memory for this by sharing some variable
	 * betwee all the solutions 
	 * TODO:
	 */
	protected Map<SetOfVars,Integer> minimalPerfectHash;
	protected int max_k;
		
	// Auxiliary data structures
	private SetOfVars affected_subfns;
	

	public RBallEfficientHillClimberForInstanceOf(RBallEfficientHillClimber rball, 
			EmbeddedLandscape problem)
	{
		this.rball=rball;
		this.problem = problem;
		initializeOperatorDependentStructures();
		initializeProblemDependentStructuresDarrell();
	}
	
	private void initializeOperatorDependentStructures() {
		variables = new int[rball.radius+1];
		indices = new int [rball.radius+1];
	}
	
	private void initializeProblemDependentStructures()
	{
		long init=System.currentTimeMillis();
		// This map is to implement a one-to-one function between SetOfVars and integers (Minimal Perfect Hashing Function)
		minimalPerfectHash = new HashMap<SetOfVars,Integer>();
		subfns = new int [problem.getM()][];
		int [][] masks = problem.getMasks();
		
		max_k = 0;
		int index=0;
		for (int sf=0; sf < subfns.length; sf++)
		{
			if (masks[sf].length > max_k)
			{
				max_k = masks[sf].length;
			}
			
			SetOfSetOfVars ssv = generateTuples(masks[sf]); 
			
			subfns[sf] = new int [ssv.size()];
			int j=0;
			for (SetOfVars sov: ssv)
			{
				Integer val = minimalPerfectHash.get(sov);
				if (val == null)
				{
					val = index;
					minimalPerfectHash.put(sov, val);
					index++;
				}
				subfns[sf][j++]=val;
			}
		}
		
		affected_subfns = new SetOfVars();
		
		problem_init_time = System.currentTimeMillis()-init;
	}
	
	private void initializeProblemDependentStructuresDarrell()
	{
		long init=System.currentTimeMillis();
		// This map is to implement a one-to-one function between SetOfVars and integers (Minimal Perfect Hashing Function)
		minimalPerfectHash = new HashMap<SetOfVars,Integer>();
		int n = problem.getN();
		int m = problem.getM();
		max_k = 0;
		//int [][] masks = problem.getMasks();
		subfns = new int [m][];
		oneFlipScores = new int [n];
		
		Set<Integer> [] aux_var_combs = new HashSet [n];
		int index=0;
		
		for (int v=0; v < aux_var_combs.length; v++)
		{
			aux_var_combs[v] = new HashSet<Integer>();
		}
		
		for (int v = 0; v < n; v++)
		{
			SetOfSetOfVars ssv = generateTuplesDarrell(v);
			for (SetOfVars sov: ssv)
			{
				Integer val = minimalPerfectHash.get(sov);
				if (val == null)
				{
					val = index;
					minimalPerfectHash.put(sov, val);
					if (sov.cardinality()==1)
					{
						oneFlipScores[v]=val;
					}
					index++;
				}
				
				for (int vv: sov)
				{
					aux_var_combs[vv].add(val);
				}
			}
		}
		
		
		Set<Integer> set = new HashSet<Integer>();
		
		for (int sf=0; sf < subfns.length; sf++)
		{
			set.clear();
			if (problem.getMaskLength(sf) /*masks[sf].length*/ > max_k)
			{
				max_k = problem.getMaskLength(sf); // masks[sf].length;
			}
			
			//for (int v: masks[sf])
			int limit = problem.getMaskLength(sf);	
			for (int v=0; v < limit; v++)
			{
				set.addAll(aux_var_combs[problem.getMasks(sf,v)] /*v*/);
			}
			
			subfns[sf] = new int [set.size()];
			
			int j=0;
			for (int e : set) {
				subfns[sf][j++] = e;
			}
		}
		
		affected_subfns = new SetOfVars();
		
		problem_init_time = System.currentTimeMillis()-init;
	}
	
	/* Problem method */
	protected Iterable<Integer> subFunctionsAffected(SetOfVars bits)
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
	
	
	private SetOfSetOfVars generateTuplesDarrell(final int v) {		
		final SetOfSetOfVars ssv = new SetOfSetOfVars();
		RootedTreeGenerator rtg = new RootedTreeGenerator();
		
		// Generate all the trees up to r and for each tree do
		rtg.generate(rball.radius, new RootedTreeCallback() {
			int [][] interactions = problem.getInteractions();
			@Override
			public void rootedTree(final int[] par, final int p) {
				// Navigate over the variables of the subfunction and plug-in the variables in the tree
				// Now, plug the variables in the tree (array "variables")
				
				if (p==1)
				{
					SetOfVars sov = new SetOfVars();
					sov.add(v);
					sov.inmute();
					ssv.add(sov);
					return;
				}
				// else
				
				variables[1] = v;
				
				new RecursiveCalls(){
					@Override
					public void run(int depth) {
						int par_var;
						int [] src;
						
						indices[depth] = 0;

						par_var = variables[par[depth]];
						src = interactions[par_var];

						for (;indices[depth]<src.length; indices[depth]++)
						{
							variables[depth] = src[indices[depth]];
							// Check if it less than the first one
							if (variables[depth] <= v)
							{
								continue;
							}
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
								for (int var=1; var <= p; var++)
								{
									sov.add(variables[var]);
								}
								sov.inmute();
								ssv.add(sov);
							}
							else
							{
								run (depth+1);
							}
						}
					}}.run(2);
			}}); 
		return ssv;
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
		rtg.generate(rball.radius, new RootedTreeCallback() {
			int [][] interactions = problem.getInteractions();
			@Override
			public void rootedTree(final int[] par, final int p) {
				// Navigate over the variables of the subfunction and plug-in the variables in the tree

				// Now, plug the variables in the tree (array "variables")
				
				variables[0] = -1;
				
				new RecursiveCalls(){
					@Override
					public void run(int depth) {
						int par_var;
						int [] src;
						
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
		
	public int getStoredScores()
	{
		return minimalPerfectHash.size();
	}
	
	public long getProblemInitTime() {
		return problem_init_time;
	}


	@Override
	public RBallEfficientHillClimberSnapshot initialize(Solution<? super EmbeddedLandscape> sol) {
		if (sol instanceof PBSolution)
		{
			return new RBallEfficientHillClimberSnapshot(this, (PBSolution)sol);
		}
		else
		{
			throw new IllegalArgumentException ("Expected argument of class PBSolution but found "+sol.getClass().getCanonicalName());
		}
	}

	@Override
	public RBallEfficientHillClimber getHillClimber() {
		return rball;
	}


	@Override
	public EmbeddedLandscape getProblem() {
		return problem;
	}

}
