package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class PartitionCrossoverArticulationPoints {

	protected static final int VARIABLE_LIMIT = 1<<29;
	
    protected Random rnd;
	protected EmbeddedLandscape el;
	protected TwoStatesIntegerSet bfsSet;
    protected Set<Integer> subfns;
    protected Queue<Integer> toExplore;
    
    protected PartitionComponent component;
    protected VariableProcedence varProcedence;

    protected PrintStream ps;
    
    private int numberOfComponents;

    public PartitionCrossoverArticulationPoints(EmbeddedLandscape el) {
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
		
		initializeDataStructures(el.getN());

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

    private int low[];
    private int time[];
    private int parent[];
    private int degree[];
    private int globalTime;
    private TwoStatesIntegerSet articulationPoints;
    private Stack<VertexIndex> stack;
    private Stack<Edge> edgeStack;
    private Set<Set<Integer>> biconnectedComponents;
    private List<Integer> degreeOfArticulationPoints;
    
    static private class VertexIndex {
        private int vertex;
        private int index;
        
        VertexIndex(int v, int i) {
            vertex = v;
            index = i;
        }
        
        static VertexIndex newVertexIndex(int v, int i) {
            return new VertexIndex(v, i);
        }
    }
    
    static private class Edge {
        private int tail;
        private int head;
        
        Edge(int tail, int head) {
            this.tail =tail;
            this.head = head;
        }
        
        static Edge newEdge(int tail, int head) {
            return new Edge(tail, head);
        }
    }
    
    private void initializeDataStructures(int n) {
        articulationPoints = new TwoStatesISArrayImpl(n);
        articulationPoints.reset();
        
        low = new int[n];
        time = new int [n];
        parent = new int [n];
        degree = new int[n];
        globalTime = 0;
        stack = new Stack<>();
        edgeStack = new Stack<>();
        biconnectedComponents = new HashSet<>();
        degreeOfArticulationPoints = new ArrayList<>();
        
    }
    
    public void setPrintStream(PrintStream ps) {
        this.ps=ps;
    }

    protected PartitionComponent dfs(Integer root, PBSolution blue, PBSolution red) {
        component.clearComponent();
        
        parent[root] = -1;
        int rootChildren = 0;
        stack.add(VertexIndex.newVertexIndex(root, 0));
        while (!stack.isEmpty()) {
            VertexIndex vi = stack.pop();
            int v = vi.vertex;
            int i = vi.index;
            if (i == 0) {
                bfsSet.explored(v);
                time[v] = globalTime++;
                low[v] = time[v];
                component.addVarToComponent(v);
            }
            if (i < el.getInteractions()[v].length) {
                stack.add(VertexIndex.newVertexIndex(v, i+1));
                if (isNodeInReducedGraph(el.getInteractions()[v][i], blue, red)) {
                    int w = el.getInteractions()[v][i];
                    if (!bfsSet.isExplored(w)) { 
                        if (parent[v] < 0) {
                            rootChildren++;
                        }
                        edgeStack.add(Edge.newEdge(v,  w));
                        stack.add(VertexIndex.newVertexIndex(w, 0));
                        parent[w] = v;

                    } else if (time[w] < time[v] && w != parent[v]) {
                        edgeStack.add(Edge.newEdge(v, w));
                        low[v] = Math.min(low[v], time[w]);
                    }
                } else {
                    bfsSet.explored(el.getInteractions()[v][i]);
                }
            } else if (!stack.isEmpty()){
                int w = v;
                vi = stack.peek();
                v = vi.vertex;
                low[v] = Math.min(low[v],low[w]);
                if (low[w] >= time[v]) {
                    if (parent[v] >= 0) {
                        if (articulationPoints.isExplored(v)) {
                            degree[v]++;
                        } else {
                            articulationPoints.explored(v);
                            degree[v] = 2;
                        }
                        
                    }
                    if (!edgeStack.isEmpty()) {
                        Set<Integer> component = new HashSet<>();
                        while (time[edgeStack.peek().tail] >= time[w]) {
                            Edge e = edgeStack.pop();
                            component.add(e.head);
                            component.add(e.tail);
                        }
                        Edge e = edgeStack.pop();
                        component.add(e.head);
                        component.add(e.tail);
                        biconnectedComponents.add(component);
                    }
                }
            }
        }

        if (rootChildren >= 2) {
            articulationPoints.explored(root);
            degree[root]=rootChildren;
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

        bfsSet.reset();
        articulationPoints.reset();
        globalTime = 0;
        biconnectedComponents.clear();
        edgeStack.clear();
        stack.clear();
        
		PBSolution child = new PBSolution(red); //child, copy of red
		numberOfComponents = 0;

		for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
		    PartitionComponent component = dfs(node, blue, red);
		    
		    if (ps!= null) {
		        printComponent(component);
		    }
		    
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
		
		if (ps!=null) {
		    printBiconnectedComponents();
            printArticulationPoints();
		}

		return child;
	}

    public int getNumberOfComponents() {
        return numberOfComponents;
    }
    
    public int getNumberOfArticulationPoints() {
        return articulationPoints.getNumberOfExploredElements();
    }
    
    private void printComponent(PartitionComponent pc) {
        Set<Integer> componentSet = new HashSet<>();
        pc.forEach(e->componentSet.add(e));
        
        ps.print("{");
        for (int u: componentSet) {
            for (int v: el.getInteractions()[u]) {
                if (u < v && componentSet.contains(v)) {
                    ps.print("("+u+","+v+"),");
                }
            }
        }
        ps.println("}");
    }
    
    private void printBiconnectedComponents() {
       ps.println(biconnectedComponents);
    }
    
    private void printArticulationPoints() {
        ps.println(articulationPoints.exploredToString());
    }
    
    public IntStream degreeOfArticulationPoints() {
        return articulationPoints.getExplored().map(i->degree[i]);
    }
}
