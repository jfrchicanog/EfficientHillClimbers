package neo.landscape.theory.apps.pseudoboolean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.Problem;
import neo.landscape.theory.apps.efficienthc.Solution;


/**
 * This class represents a Pseudo-Boolean Function. It contains m subfunctions.
 * This class is an abstraction for all additively decomposables Pseudo Boolean Functions.
 * @author francis
 *
 */

public abstract class AdditivelyDecomposablePBF extends PseudoBooleanFunction {

	protected int m;
	protected int [][] masks;
	private int [][] appearsIn;
	private int [][] interactions;
	private PBSolution sub;

	public AdditivelyDecomposablePBF() {
		super();
	}

	/**
	 * this function should be prepared to receive a solution with more bits than require and take only the required. This is an efficiency measure.
	 * @param sf
	 * @param pbs
	 * @return
	 */
	
	public abstract double evaluateSubfunction(int sf, PBSolution pbs);

	@Override
	public double evaluate(Solution sol) {
		PBSolution pbs = (PBSolution)sol;
		
		double res = 0;
		// Build the subsolution
		
		for (int i=0; i < m; i++)
		{	
			for (int j=0; j < masks[i].length; j++)
			{
				sub.setBit(j,pbs.getBit(masks[i][j]));
			}
			// Evaluate
			res += evaluateSubfunction (i, sub);
		}
		
		return res;
	}

	private void prepareStructures() {
		List<Integer> [] aux = new List[n];
		int max_length=0;
		
		for (int sf=0; sf < m; sf++)
		{
			if (masks[sf].length > max_length)
			{
				max_length = masks[sf].length;
			}
			
			for (int i=0; i < masks[sf].length; i++)
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
		
		sub = new PBSolution (max_length);
	}

	/**
	 * The appearsIn array (don't modify this structure please!)
	 * @return
	 */
	public int [][] getAppearsIn() {
		if (appearsIn == null)
		{
			prepareStructures();
		}
		
		return appearsIn;
	}

	/**
	 * The interactions array (don't modify this structure please!)
	 * @return
	 */
	public int [][] getInteractions() {
		if (interactions == null)
		{
			prepareStructures();
		}
		
		return interactions;
	}

	public int [][] getMasks() {
		return masks;
	}

	public int getM() {
		return m;
	}

}