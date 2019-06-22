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

public class NetworkCrossover implements CrossoverInternal {

	protected static final int VARIABLE_LIMIT = 1<<29;
	
    protected Random rnd;
	protected EmbeddedLandscape el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected LinkedList<Integer> toExplore;
    
    protected PartitionComponent component;
    protected VariableProcedence varProcedence;
    
    protected int maximumSizeOfMask;
    
    protected long lastRuntime;

    private int numberOfComponents;
    
    private PrintStream ps;
    
    private int baseSolution;
    private int sizeOfMask;

    public NetworkCrossover(EmbeddedLandscape el) {
		this.el = el;
		rnd = new Random(Seeds.getSeed());
		bfsSet = new TwoStatesISArrayImpl(el.getN(), rnd.nextLong());
		if (el.getN() > VARIABLE_LIMIT) {
		    throw new RuntimeException("Solution too large, the maximum allowed is "+VARIABLE_LIMIT);
		}

		subfns = new HashSet<Integer>();
		toExplore = new LinkedList<Integer>();
		ComponentAndVariableMask componentAndVariableProcedence = new ComponentAndVariableMask(el.getN());
		component = componentAndVariableProcedence;
		varProcedence = componentAndVariableProcedence;

	}

    public void setSeed(long seed) {
		rnd = new Random(seed);
		bfsSet = new TwoStatesISArrayImpl(el.getN(), rnd.nextLong());
	}

	protected void colorVariable(int v, PBSolution blue, PBSolution red) {
		if (blue.getBit(v) != red.getBit(v)) {
	    	if (baseSolution==VariableProcedence.BLUE) {
	    		varProcedence.markAsBlue(v);
	    	} else {
	    		varProcedence.markAsRed(v);
	    	}
	    } else {
	        varProcedence.markAsPurple(v);
	    }
	}

	/**
	 * Search for the next node in the reduced graph
	 * 
	 * @param blue
	 * @param red
	 * @return
	 */
	protected Integer nextNodeInVariableInteractionGraph(PBSolution blue, PBSolution red) {

		if (bfsSet.hasMoreUnexplored()) {
			return bfsSet.getRandomUnexplored();
		}

		return null;
	}

    protected PartitionComponent bfs(Integer node, PBSolution blue, PBSolution red) {

		toExplore.clear();		
		component.clearComponent();
		
		toExplore.add(node);
		colorVariable(node, blue, red);
		
		while (toExplore.size() > 0 && sizeOfMask < maximumSizeOfMask) {
			// Take one node to explore (randomly)
			int var = toExplore.get(rnd.nextInt(toExplore.size()));
			if (bfsSet.isExplored(var)) {
				continue;
			}

			component.addVarToComponent(var);
			
			// Enumerate the adjacent variables
			for (int adj : el.getInteractions()[var]) {
				colorVariable(adj, blue, red);
				if (!bfsSet.isExplored(adj)) {
					toExplore.add(adj);
				}
			}

			bfsSet.explored(var);
			sizeOfMask++;
		}

		return component;
	}

	public PBSolution recombine(PBSolution blue, PBSolution red) {
	    PBSolution solution = recombineInternal(blue, red);
	    ps.println("Recombination time:"+getLastRuntime());
	    
	    return solution;
	}
	
	@Override
	public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
		long initTime = System.nanoTime();
        bfsSet.reset();
        
        baseSolution = rnd.nextBoolean()?VariableProcedence.BLUE:VariableProcedence.RED;
        
		PBSolution child = new PBSolution((baseSolution==VariableProcedence.RED)?red:blue); //child, copy of red
		
		numberOfComponents = 0;
		sizeOfMask=0;

		for (Integer node = nextNodeInVariableInteractionGraph(blue, red); sizeOfMask < maximumSizeOfMask && node != null; node = nextNodeInVariableInteractionGraph(blue, red)) {
		    PartitionComponent component = bfs(node, blue, red);
		    
		    for (int variable : component) {
		    	if (varProcedence.getColor(variable)!=VariableProcedence.PURPLE) { 
		    		child.flipBit(variable);
		    		if (baseSolution==VariableProcedence.RED) {
		    			varProcedence.markAsBlue(variable);
		    		} else {
		    			varProcedence.markAsRed(variable);
		    		}
		    	}
		    }

			numberOfComponents++;
		}
		
		bfsSet.getUnexplored().forEach(variable -> colorVariable(variable, blue, red));
		
		lastRuntime = System.nanoTime() - initTime;
		
		if (ps != null) {
			ps.println("* Number of components: "+getNumberOfComponents());
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

	@Override
	public EmbeddedLandscape getEmbddedLandscape() {
		return el;
	}

	@Override
	public VariableProcedence getVarProcedence() {
		return varProcedence;
	}

	public int getMaximumSizeOfMask() {
		return maximumSizeOfMask;
	}

	public void setMaximumSizeOfMask(int maximumSizeOfMask) {
		this.maximumSizeOfMask = maximumSizeOfMask;
	}

}
