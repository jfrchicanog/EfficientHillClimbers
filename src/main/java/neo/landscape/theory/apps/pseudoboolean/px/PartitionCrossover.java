package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class PartitionCrossover {

	protected static final int VARIABLE_LIMIT = 1<<29;
	
    protected Random rnd;
	protected EmbeddedLandscape el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected Queue<Integer> toExplore;
    
    protected PartitionComponent component;
    protected VariableProcedence varProcedence;
    
    protected long lastRuntime;

    private int numberOfComponents;
    
    private PrintStream ps;

    public PartitionCrossover(EmbeddedLandscape el) {
		this.el = el;
		bfsSet = new TwoStatesISArrayImpl(el.getN());
		if (el.getN() > VARIABLE_LIMIT) {
		    throw new RuntimeException("Solution too large, the maximum allowed is "+VARIABLE_LIMIT);
		}
		
		rnd = new Random(Seeds.getSeed());
		subfns = new HashSet<Integer>();
		toExplore = new LinkedList<Integer>();
		ComponentAndVariableMask componentAndVariableProcedence = new ComponentAndVariableMask(el.getN());
		component = componentAndVariableProcedence;
		varProcedence = componentAndVariableProcedence;

	}

    public void setSeed(long seed) {
		rnd = new Random(seed);
	}

	protected boolean isNodeInReducedGraph(int v, PBSolution blue,
			PBSolution red) {
	    if (blue.getBit(v) != red.getBit(v)) {
	        varProcedence.markAsRed(v);
	        return true;
	    } else {
	        varProcedence.markAsPurple(v);
	        return false;
	    }
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
			if (isNodeInReducedGraph(v, blue, red)) {
				return v;
			} else {
				bfsSet.explored(v);
			}
		}

		return null;
	}

    protected PartitionComponent bfs(Integer node, PBSolution blue, PBSolution red) {

		toExplore.clear();		
		component.clearComponent();
		
		toExplore.add(node);
		
		while (toExplore.size() > 0) {
			// Take one node to explore
			int var = toExplore.remove();
			if (bfsSet.isExplored(var)) {
				continue;
			}

			component.addVarToComponent(var);
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

		return component;
	}

    protected double[] evaluateComponent(Iterable<Integer> vars, PBSolution... sol) {
		double[] res = new double[sol.length];

		subfns.clear();

		for (int variable : vars) {
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
	    long initTime = System.nanoTime();
        bfsSet.reset();
        
		PBSolution child = new PBSolution(red); //child, copy of red
		numberOfComponents = 0;

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    PartitionComponent component = bfs(node, blue, red);
			double[] vals = evaluateComponent(component, blue, red);
			
			double blueVal = vals[0];
			double redVal = vals[1];

			if (blueVal > redVal || ((blueVal==redVal) && rnd.nextDouble() < 0.5)) {
			    for (int variable : component) {
			        child.setBit(variable, blue.getBit(variable));
                    varProcedence.markAsBlue(variable);
                }
			}
			numberOfComponents++;
		}
		lastRuntime = System.nanoTime() - initTime;
		ps.println("Recombination time:"+getLastRuntime());
		if (child != null) {
			ps.println("* Success in PX: "+getNumberOfComponents());
		}
		return child;
	}

    public int getNumberOfComponents() {
        return numberOfComponents;
    }
    
    public long getLastRuntime() {
        return lastRuntime;
    }
    
	public void setPrintStream(PrintStream ps) {
		this.ps = ps;
	}
	
	public void setDebug(boolean debug) {
	}

}
