package neo.landscape.theory.apps.pseudoboolean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.Problem;
import neo.landscape.theory.apps.efficienthc.Solution;

/**
 * This class represents a K-bounded epistasis Pseudo-Boolean Function. It contains m subfunctions with k bits each.
 * This class is an abstraction for NK-landscapes and MAX-k-SAT
 * @author francis
 *
 */

public abstract class KBoundedEpistasisPBF implements Problem {
	
	protected Random rnd;
	protected int m; // number of subfunctions
	protected int k; // epistasis degree
	protected int n; // number of bits of a string , the variables will be enumerated from 0 to n-1
	protected int [][] masks;  // masks for the subfunctions
	 
	// Statistics for the variables
	private int [][] appearsIn;
	// For each variable here it is the list of variables with which interacts
	private int [][] interactions; 
	
	public KBoundedEpistasisPBF()
	{
		rnd = new Random();
	}

	@Override
	public void setSeed(long seed) {
		rnd = new Random (seed);
	}

	@Override
	public PBSolution getRandomSolution() {
		PBSolution pbs = new PBSolution (n);
		
		for (int i=0; i < n; i++)
		{
			pbs.setBit(i, rnd.nextInt(2));
		}
	
		return pbs;
	}
	
	public abstract double evaluateSubfunction (int sf, PBSolution pbs);


	@Override
	public double evaluate(Solution sol) {
		PBSolution pbs = (PBSolution)sol;
		
		double res = 0;
		
		for (int i=0; i < m; i++)
		{
			// Build the subsolution
			PBSolution sub = new PBSolution(k);
			for (int j=0; j < k; j++)
			{
				sub.setBit(j,pbs.getBit(masks[i][j]));
			}
			// Evaluate
			res += evaluateSubfunction (i, sub);
		}
		
		return res;
	}
	
	private void prepareStructures()
	{
		List<Integer> [] aux = new List[n];
		
		for (int sf=0; sf < m; sf++)
		{
			for (int i=0; i < k; i++)
			{
				int var = masks[sf][i];
				if (aux[var]==null)
				{
					aux[var] = new ArrayList<Integer>();
				}
				aux[var].add(sf);
			}
		}
		
		appearsIn = new int [n][];
		for (int var=0; var < n; var++)
		{
			int size = (aux[var]==null)?0:aux[var].size();
			appearsIn[var] = new int[size];
			for (int i=0; i < size; i++)
			{
				appearsIn[var][i] = aux[var].get(i);
			}
		}
		
		interactions = new int[n][];
		
		Set<Integer> aux_inter=new HashSet<Integer>();
		for (int i=0; i < n; i++)
		{
			aux_inter.clear();
			for (int sf : appearsIn[i])
			{
				for (int var: masks[sf])
				{
					aux_inter.add(var);
				}
			}
			aux_inter.remove(i);
			
			interactions[i] = new int [aux_inter.size()];
			int j=0;
			for (int var: aux_inter)
			{
				interactions[i][j]=var;
				j++;
			}
			
		}
	}
	
	/**
	 * The appearsIn array (don't modify this structure please!)
	 * @return
	 */
	public int [][] getAppearsIn()
	{
		if (appearsIn == null)
		{
			prepareStructures();
		}
		
		return appearsIn;
	}
	
	public int [][] getInteractions()
	{
		if (interactions == null)
		{
			prepareStructures();
		}
		
		return interactions;
	}
	
	public int [][] getMasks()
	{
		return masks;
	}
	
	public int getK()
	{
		return k;
	}

	public int getM() {
		return m;
	}

	public int getN() {
		return n;
	}

}
