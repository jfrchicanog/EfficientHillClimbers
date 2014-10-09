package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class PartitionCrossover {
	
	protected Random rnd;

	protected EmbeddedLandscape el;
	protected TwoStatesIntegerSet bfsSet;
	
	public PartitionCrossover(EmbeddedLandscape el)
	{
		this.el=el;
		bfsSet = new TwoStatesISArrayImpl(el.getN());
		rnd = new Random (Seeds.getSeed());
	}
	
	public void setSeed(long seed)
	{
		rnd = new Random (seed);
	}
	
	protected boolean isNodeInReducedGraph(int v, PBSolution blue, PBSolution red)
	{
		return blue.getBit(v) != red.getBit(v);
	}
	
	/**
	 * Search for the net node in the reduced graph	
	 * @param blue
	 * @param red
	 * @return
	 */
	protected Integer nextNodeInReducedGraph(PBSolution blue, PBSolution red)
	{
		int v;
		
		while (bfsSet.hasMoreUnexplored())
		{
			v = bfsSet.getNextUnexplored();
			if (isNodeInReducedGraph(v, blue, red))
			{
				return v;
			}
			else
			{
				bfsSet.explored(v);
			}
		}
		
		return null;
	}
	
	protected List<Set<Integer>> computePartition(PBSolution blue, PBSolution red)
	{
		bfsSet.reset();
		List<Set<Integer>> res = new ArrayList<Set<Integer>>();
		
		for (Integer node = nextNodeInReducedGraph(blue, red);
				node != null;
				node = nextNodeInReducedGraph(blue, red))
		{
			// Apply Bread First Search to compute the component
			res.add(bfs(node, blue, red));
		}
		return res;
	}
	
	protected Set<Integer> bfs(Integer node, PBSolution blue, PBSolution red) {
		
		Queue<Integer> toExplore = new LinkedList<Integer>();
		toExplore.add(node);
		Set<Integer> res = new HashSet<Integer>();
		
		while (toExplore.size() > 0)
		{
			// Take one node to explore
			int var = toExplore.remove();
			if (bfsSet.isExplored(var))
			{
				continue;
			}
			
			res.add(var);
			// Enumerate the adjacent variables
			for (int adj: el.getInteractions()[var])
			{
				if (bfsSet.isExplored(adj) || !isNodeInReducedGraph(adj, blue, red))
				{
					// Omit this variable (but mark it as explored in the case that is not in the reduced graph)
					bfsSet.explored(adj);
				}
				else
				{
					toExplore.add(adj);
				}
			}
			
			bfsSet.explored(var);
		}
		
		return res;
	}

	protected double [] evaluateComponent(Set<Integer> vars, PBSolution ... sol)
	{
		double [] res = new double [sol.length];
		
		Set<Integer> subfns = new HashSet<Integer>();
		
		for (int v: vars)
		{
			int [] fns = el.getAppearsIn()[v];
			for (int i = 0; i < fns.length; i++) {
				subfns.add(fns[i]);
			}
		}
		
		for (int i = 0; i < res.length; i++) {
			res[i]=0.0;
			for (int fn: subfns)
			{
				res[i] += el.evaluateSubFunctionFromCompleteSolution(fn, sol[i]);
			}
		}
		
		return res;
	}
	
	public PBSolution recombine (PBSolution blue, PBSolution red)
	{
		List<Set<Integer>> partition= computePartition(blue, red);
		List<Set<Integer>> takenFromBlue =  new ArrayList<Set<Integer>>();
		List<Set<Integer>> takenFromRed = new ArrayList<Set<Integer>>();
		
		int blue_vars = 0;
		int red_vars = 0;
	
		for (Set<Integer> component: partition)
		{
			double [] vals = evaluateComponent(component, blue, red);
			double blue_val = vals[0];
			double red_val = vals[1];
			
			if (blue_val > red_val)
			{
				takenFromBlue.add(component);
				blue_vars += component.size();
			}
			else if (red_val > blue_val)
			{
				takenFromRed.add(component);
				red_vars += component.size();
			}
			else
			{
				if (rnd.nextDouble() < 0.5)
				{
					takenFromBlue.add(component);
					blue_vars += component.size();
				}
				else
				{
					takenFromRed.add(component);
					red_vars += component.size();
				}
			}
		}
		
		List<Set<Integer>> takenFromTheOther;
		PBSolution theOther;
		PBSolution res;
		
		if (blue_vars > red_vars)
		{
			res = new PBSolution(blue);
			theOther = red;
			takenFromTheOther = takenFromRed;
		}
		else
		{
			res = new PBSolution(red);
			theOther = blue;
			takenFromTheOther = takenFromBlue;
		}
		
		for (Set<Integer> comp: takenFromTheOther)
		{
			for (int v: comp)
			{
				res.setBit(v, theOther.getBit(v));
			}
		}
		
		return res;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
