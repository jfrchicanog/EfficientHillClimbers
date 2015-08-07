package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class PartitionCrossover {

	protected Random rnd;
	protected EmbeddedLandscape el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected Queue<Integer> toExplore;
    
    protected int [] componentOfPartition;
    protected int sizeOfComponent;

	public PartitionCrossover(EmbeddedLandscape el) {
		this.el = el;
		bfsSet = new TwoStatesISArrayImpl(el.getN());
		if (el.getN() > (1<<29)) {
		    throw new RuntimeException("Solution too large, the maximum allowed is "+(1<<29));
		}
		
		rnd = new Random(Seeds.getSeed());
		subfns = new HashSet<Integer>();
		toExplore = new LinkedList<Integer>();
        componentOfPartition = new int [el.getN()];
        sizeOfComponent=0;
	}

	public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	protected boolean isNodeInReducedGraph(int v, PBSolution blue,
			PBSolution red) {
		return blue.getBit(v) != red.getBit(v);
	}

	/**
	 * Search for the net node in the reduced graph
	 * 
	 * @param blue
	 * @param red
	 * @return
	 */
	protected Integer nextNodeInReducedGraph(PBSolution blue, PBSolution red) {
		int v;

		while (bfsSet.hasMoreUnexplored()) {
			v = bfsSet.getNextUnexplored();
			if (blue.getBit(v) != red.getBit(v)) {
			    // Mark that the bits comes from the red solution
			    componentOfPartition[v] &= (~0x3);
			    componentOfPartition[v] |= 0x1;
				return v;
			} else {
				bfsSet.explored(v);
				// Mark that both solutions have the same value
				componentOfPartition[v] |= 0x3;
			}
		}

		return null;
	}

	protected int [] bfs(Integer node, PBSolution blue, PBSolution red) {

		toExplore.clear();		
		sizeOfComponent = 0;
		
		toExplore.add(node);
		
		while (toExplore.size() > 0) {
			// Take one node to explore
			int var = toExplore.remove();
			if (bfsSet.isExplored(var)) {
				continue;
			}

			componentOfPartition[sizeOfComponent++] = (var << 2);
			// Enumerate the adjacent variables
			for (int adj : el.getInteractions()[var]) {
				if (bfsSet.isExplored(adj)
						|| !isNodeInReducedGraph(adj, blue, red)) {
					// Omit this variable (but mark it as explored in the case
					// that is not in the reduced graph)
					bfsSet.explored(adj);
				} else {
					toExplore.add(adj);
				}
			}

			bfsSet.explored(var);
		}

		return componentOfPartition;
	}

	protected double[] evaluateComponent(int [] vars, PBSolution... sol) {
		double[] res = new double[sol.length];

		subfns.clear();

		for (int v : vars) {
		    int variable = (v >>> 2);
			int[] fns = el.getAppearsIn()[variable];
			for (int i = 0; i < fns.length; i++) {
				subfns.add(fns[i]);
			}
		}

		for (int i = 0; i < res.length; i++) {
			res[i] = 0.0;
			for (int fn : subfns) {
				res[i] += el.evaluateSubFunctionFromCompleteSolution(fn, sol[i]);
			}
		}

		return res;
	}

	public PBSolution recombine(PBSolution blue, PBSolution red) {

        bfsSet.reset();
        
		PBSolution child = new PBSolution(red); //child, copy of red

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    int [] component = bfs(node, blue, red);
			double[] vals = evaluateComponent(component, blue, red);
			
			double blueVal = vals[0];
			double redVal = vals[1];

			if (blueVal > redVal || ((blueVal==redVal) && rnd.nextDouble() < 0.5)) {
			    for (int v : component) {
			        int variable = (v >>> 2);
                    child.setBit(variable, blue.getBit(variable));
                    // Mark that the bits come from the blue solution
                    componentOfPartition[variable] &= (~0x3);
                    componentOfPartition[variable] |= 0x2;
                }
			}
		}

		return child;
	}



}
