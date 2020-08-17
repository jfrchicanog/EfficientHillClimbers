package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.MemoryEfficientUndirectedGraph;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.UndirectedGraph;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.UndirectedGraphFactory;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableCliqueImplementation;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class DynasticPotentialCrossover implements CrossoverInternal {
	
	private static final int DEFAULT_MAXIMUM_VARIABLES_TO_EXPLORE = 28;
	protected static final int VARIABLE_LIMIT = 1<<29;
	protected EmbeddedLandscape el;
    
	int maximumNumberOfVariableToExploreExhaustively = DEFAULT_MAXIMUM_VARIABLES_TO_EXPLORE;
	
	int [] alpha;
	private int [] alphaInverted;
	private Set<Integer> [] verticesWithNMarks;
	int [] marks;
	private boolean differentSolutions;
	private int topLabel;
	private int initialLabel;
	// Chordal graph
	private UndirectedGraph chordalGraph;
	private UndirectedGraphFactory graphFactory = MemoryEfficientUndirectedGraph.FACTORY;
	// Clique tree 
	private CliqueManagementFactory cmFactory = CliqueManagementMemoryEfficient.FACTORY;
	public CliqueManagement cliqueManagement;
	// Subfunctions
	private List<Integer> [] subFunctionsPartition;
	private TwoStatesIntegerSet subfunctions;
	
    protected long lastRuntime;

    private int numberOfComponents;
	private int [] fFillin;
	private int [] indexFillin;
	private List<Integer> [] mSets;
	private int [] cliqueOfVariable;
	private int [] last;
	
	private PBSolution red;
	private PBSolution blue;
	
	protected VariableProcedence varProcedence;
	protected PartitionComponent component;
	
	protected PrintStream ps;
	protected boolean debug;
	public DynasticPotentialCrossover(EmbeddedLandscape el) {
		int n = el.getN();
		int maxDegree = el.getMaximumDegreeOfVIG();
		
		alpha = new int [n];
		topLabel = n-1;
		alphaInverted = new int [topLabel+1];
		verticesWithNMarks = new Set[maxDegree+1];
		marks = new int [n];
		for (int i=0; i < verticesWithNMarks.length; i++) {
			verticesWithNMarks[i] = new HashSet<>();
		}
		chordalGraph = graphFactory.createGraph(n);
		fFillin = new int[n];
		indexFillin = new int [n];
		mSets = new List[n];
		for (int i=0; i <n; i++) mSets[i] = new ArrayList<>();
		cliqueOfVariable = new int [n];
		cliqueManagement = cmFactory.createCliqueManagement(n);
		last = new int[n];
		subFunctionsPartition = new List[n];
		for (int i=0; i < n; i++) {
			subFunctionsPartition[i] = new ArrayList<>();
		}
		subfunctions = new TwoStatesISArrayImpl(el.getM());
		
		this.el = el;
		
		if (el.getN() > VARIABLE_LIMIT) {
		    throw new RuntimeException("Solution too large, the maximum allowed is "+VARIABLE_LIMIT);
		}
		
		ComponentAndVariableMask componentAndVariableProcedence = new ComponentAndVariableMask(el.getN());
		component = componentAndVariableProcedence;
		varProcedence = componentAndVariableProcedence;
	}

	private void maximumCardinalitySearch() {
		int n = el.getN();
		
		for (int i=0; i < verticesWithNMarks.length; i++) {
			verticesWithNMarks[i].clear();
		}
		
		differentSolutions = false;
		
		for (int i=0; i < n; i++) {
			varProcedence.markAsPurple(i);
		}
		
		IntStream.range(0, n).filter(v -> (blue.getBit(v) != red.getBit(v))).forEach(vertex -> 
			{marks[vertex] = 0; 
			verticesWithNMarks[0].add(vertex); 
			differentSolutions=true;
			varProcedence.markAsRed(vertex);
			}
		);
		
		if (!differentSolutions) return;
		
		int i=topLabel;
		initialLabel = i;
		int j=0;
		while (j>=0) {
			int vertex = verticesWithNMarks[j].iterator().next();
			verticesWithNMarks[j].remove(vertex);
			alpha[vertex] = i;
			alphaInverted[i] = vertex;
			initialLabel=i;
			marks[vertex] = -1;
			
			for (int w : el.getInteractions()[vertex]) {
				if (blue.getBit(w) != red.getBit(w)) {
					if (marks[w] >= 0) {
						verticesWithNMarks[marks[w]].remove(w);
						marks[w]++;
						verticesWithNMarks[marks[w]].add(w);
					}
				}
			}
			i--;
			j++;
			for (;j>=0 && verticesWithNMarks[j].isEmpty();j--);
		}
	}
	
	private void maximumCardinalitySearchBasedOnChordalGraph() {
		int n = el.getN();
		for (int i=0; i < verticesWithNMarks.length; i++) {
			verticesWithNMarks[i].clear();
		}
		
		for (int vertex: chordalGraph.getNodes()) {
			marks[vertex] = 0; 
			verticesWithNMarks[0].add(vertex);
		}

		int i=topLabel;
		initialLabel = i;
		int j=0;
		while (j>=0) {
			int vertex = verticesWithNMarks[j].iterator().next();
			verticesWithNMarks[j].remove(vertex);
			alpha[vertex] = i;
			alphaInverted[i] = vertex;
			initialLabel=i;
			marks[vertex] = -1;
			
			for (int w : chordalGraph.getAdjacent(vertex)) {
				if (marks[w] >= 0) {
					verticesWithNMarks[marks[w]].remove(w);
					marks[w]++;
					verticesWithNMarks[marks[w]].add(w);
				}
			}
			i--;
			j++;
			for (;j>=0 && verticesWithNMarks[j].isEmpty();j--);
		}
	}
	
	private void computeSubfunctinsPartition() {
		subfunctions.reset();
		for (int i=initialLabel; i <= topLabel; i++) {
			int vertex = alphaInverted[i];
			subFunctionsPartition[vertex].clear();
			for (int fn: el.getAppearsIn()[vertex]) {
				if (!subfunctions.isExplored(fn)) {
					/*
					int minIndex = topLabel;
					for (int var: el.getMasks()[fn]) {
						if (blue.getBit(var) != red.getBit(var)) {
							if (alpha[var] < minIndex) {
								minIndex= alpha[var];
							}
						}
					}*/
					subFunctionsPartition[vertex].add(fn);
					subfunctions.explored(fn);
				}
			}
		}
	}
	
	private void fillIn() {
		chordalGraph.clearGraph();
		for (int i=initialLabel; i <= topLabel; i++) {
			int w = alphaInverted[i];
			chordalGraph.addNodeToGraph(w);
			fFillin[w] = w;
			indexFillin[w] = i;
			for (int v : el.getInteractions()[w]) {
				if (blue.getBit(v) != red.getBit(v)) {
					if (alpha[v] < i) {
						int x=v;
						while (indexFillin[x] < i) {
							indexFillin[x] = i;
							chordalGraph.addEdgeToGraph(x, w);
							x = fFillin[x];
						}
						if (fFillin[x]==x) {
							fFillin[x] = w;
						}
					}
				}
			}
		}
	}
	
	private void cliqueTree() {
		numberOfComponents=1;
		int n = el.getN();
		for (int i=topLabel; i>=initialLabel; i--) {
			int x = alphaInverted[i];
			marks[x]=0;
			last[x] = -1;
		}
		
		int previousMark = -1;
		
		VariableClique currentClique=cliqueManagement.addNewVariableClique();
		
		for (int i=topLabel; i>=initialLabel; i--) {
			int x = alphaInverted[i];
			if (marks[x] <= previousMark) {
				currentClique=cliqueManagement.addNewVariableClique();
				currentClique.addAllVariables(mSets[x]);
				currentClique.markSeparator();
				currentClique.addVariable(x);
				
				if (currentClique.getVariablesOfSeparator() == 1) {
					cliqueManagement.addArticulationPoint(currentClique.getVariable(0));
				}
				
				if (last[x] >= 0) {
					cliqueManagement.setVariableCliqueParent(currentClique.getId(), cliqueOfVariable[last[x]]);
				} else {
					numberOfComponents++;
				}
					
			} else {
				currentClique.addVariable(x);
			}
			for (Integer y: chordalGraph.getAdjacent(x)) {
				mSets[y].add(x);
				marks[y]++;
				last[y] = x;
			}
			previousMark = marks[x];
			cliqueOfVariable[x] = currentClique.getId();
		}
		for (int i=topLabel; i>=initialLabel; i--) {
			// Cleaning memory, for the GC to work well
			int x = alphaInverted[i];
			mSets[x].clear();
		}
	}

	public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
	    long initTime = System.nanoTime();
	    System.out.println("DPX starts: "+0);
	    this.red = red;
	    this.blue = blue;
	    
	    PBSolution child = new PBSolution(red); //child, copy of red
	    
	    maximumCardinalitySearch();
	    System.out.println("Maximum cardinality search finished at: "+(System.nanoTime()-initTime));
	    
	    numberOfComponents = 0;
	    cliqueManagement.clearCliqueTree();
	    
	    if (differentSolutions) {
	    	fillIn();
	    	System.out.println("Fill in finished at: "+(System.nanoTime()-initTime));
	    	maximumCardinalitySearchBasedOnChordalGraph();
	    	System.out.println("New MCS finished at: "+(System.nanoTime()-initTime));
	    	computeSubfunctinsPartition();
	    	System.out.println("Subfunctions organization finished at: "+(System.nanoTime()-initTime));
		    cliqueTree();
		    System.out.println("Clique tree computation finished at: "+(System.nanoTime()-initTime));
		    cliqueManagement.cliqueTreeAnalysis(this);
		    System.out.println("Clique tree analysis finished at: "+(System.nanoTime()-initTime));
		    if (debug && ps != null) {
		    	ps.println("Initial label: "+initialLabel);
		    	ps.println("Number of components: "+numberOfComponents);
		    	ps.println(cliqueManagement.getCliqueTree());
		    }
		    cliqueManagement.applyDynamicProgramming(cliqueManagement.getNonExhaustivelyExploredVariables(), marks, red, el, subFunctionsPartition);
		    System.out.println("Dynamic programming finished at: "+(System.nanoTime()-initTime));
		    cliqueManagement.reconstructOptimalChild(child, red, varProcedence);
	    }

		lastRuntime = System.nanoTime() - initTime;
		System.out.println("DPX finishes at: "+lastRuntime);
		
		ps.println("* Number of components: "+getNumberOfComponents());
		int logarithmOfExploredSolutions = getLogarithmOfExploredSolutions();
		ps.println("* Logarithm of explored solutions: " + logarithmOfExploredSolutions);
		ps.println("* Full dynastic potential explored: "
				+ (getDifferingVariables() == logarithmOfExploredSolutions));
		ps.println("* Number of articulation points: " + getNumberOfArticulationPoints());
		ps.println("* All articulation points exhaustively explored: "
				+ cliqueManagement.allArticulationPointsExhaustivelyExplored(this));
		
		return child;
	}
	
	public PBSolution recombine(PBSolution blue, PBSolution red) {
	    PBSolution solution = recombineInternal(blue, red);
	    if (ps != null) {
	    	ps.println("Recombination time:"+getLastRuntime());
	    }
	    return solution;
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
		this.debug = debug;
	}
	
	public void setMaximumVariablesToExhaustivelyExplore(int numberOfVariables) {
		if (numberOfVariables > DEFAULT_MAXIMUM_VARIABLES_TO_EXPLORE) {
			throw new IllegalArgumentException("The number of variables to explore exhaustively is too large: "+numberOfVariables);
		}
		maximumNumberOfVariableToExploreExhaustively = numberOfVariables;
	}

	public int getLogarithmOfExploredSolutions() {
		return cliqueManagement.getGroupsOfNonExhaustivelyExploredVariables() + (getDifferingVariables()-cliqueManagement.getNonExhaustivelyExploredVariables().getNumberOfExploredElements());
	}
	
	public int getDifferingVariables() {
		return differentSolutions?(topLabel-initialLabel+1):0;
	}
	
	public int getNumberOfArticulationPoints() {
		return cliqueManagement.getNumberOfArticulationPoints();
	}
	
	public EmbeddedLandscape getEmbddedLandscape() {
		return el;
	}

	public VariableProcedence getVarProcedence() {
		return varProcedence;
	}

	@Override
	public void setSeed(long seed) {
	}
	
	
    
}
