package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.MemoryEfficientUndirectedGraph;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.UndirectedGraph;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.UndirectedGraphFactory;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class DynasticPotentialCrossover implements CrossoverInternal {
	
	private static class IndexAssigner implements Function<Integer,Integer> {
		private int index=0;
		@Override
		public Integer apply(Integer arraySize) {
			int thisIndex = index;
			index += arraySize;
			return thisIndex;
		}
		public int getIndex() {
			return index;
		}
		
		public void clearIndex() {
			index=0;
		}
	}
	
	private final IndexAssigner indexAssigner = new IndexAssigner();
	
	private static final int DEFAULT_MAXIMUM_VARIABLES_TO_EXPLORE = 28;
	protected static final int VARIABLE_LIMIT = 1<<29;
	protected EmbeddedLandscape el;
    
	private int maximumNumberOfVariableToExploreExhaustively = DEFAULT_MAXIMUM_VARIABLES_TO_EXPLORE;
	
	private int [] alpha;
	private int [] alphaInverted;
	private Set<Integer> [] verticesWithNMarks;
	private int [] marks;
	private boolean differentSolutions;
	private int topLabel;
	private int initialLabel;
	// Chordal graph
	private UndirectedGraph chordalGraph;
	private UndirectedGraphFactory graphFactory = MemoryEfficientUndirectedGraph.FACTORY;
	// Clique Tree
	private List<VariableClique> cliques;
	private double [] summaryValue;
	private int [] variableValue;
	// Subfunctions
	private List<Integer> [] subFunctionsPartition;
	private TwoStatesIntegerSet subfunctions;
	
    protected long lastRuntime;

    private int numberOfComponents;
	private int [] fFillin;
	private int [] indexFillin;
	private Set<Integer> [] mSets;
	private VariableClique [] cliqueOfVariable;
	private int [] last;
	
	private Set<Integer> articulationPoints;
	private Set<Integer> nonExhaustivelyExploredVariables;
	private int groupsOfNonExhaustivelyExploredVariables;
	
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
		mSets = new Set[n];
		for (int i=0; i <n; i++) mSets[i] = new HashSet<>();
		cliques = new ArrayList<>();
		cliqueOfVariable = new VariableClique [n];
		last = new int[n];
		subFunctionsPartition = new List[n];
		for (int i=0; i < n; i++) {
			subFunctionsPartition[i] = new ArrayList<>();
		}
		subfunctions = new TwoStatesISArrayImpl(el.getM());
		articulationPoints = new HashSet<>();
		nonExhaustivelyExploredVariables = new HashSet<>();
		
		
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
		articulationPoints.clear();
		numberOfComponents=1;
		int n = el.getN();
		cliques.clear();
		for (int i=0; i <n; i++) {
			mSets[i].clear();
			marks[i]=0;
			last[i] = -1;
		}
		
		int previousMark = -1;
		int j=0;
		
		VariableClique currentClique=new VariableClique(j);
		cliques.add(currentClique);
		
		for (int i=topLabel; i>=initialLabel; i--) {
			int x = alphaInverted[i];
			if (marks[x] <= previousMark) {
				j++;
				currentClique=new VariableClique(j);
				cliques.add(currentClique);
				
				currentClique.getVariables().addAll(mSets[x]);
				currentClique.markSeparator();
				currentClique.getVariables().add(x);
				
				if (currentClique.getVariablesOfSeparator() == 1) {
					articulationPoints.add(currentClique.getVariables().get(0));
				}
				
				if (last[x] >= 0) {
					currentClique.setParent(cliqueOfVariable[last[x]]);
				} else {
					numberOfComponents++;
				}
					
			} else {
				currentClique.getVariables().add(x);
			}
			for (Integer y: chordalGraph.getAdjacent(x)) {
				mSets[y].add(x);
				marks[y]++;
				last[y] = x;
			}
			previousMark = marks[x];
			cliqueOfVariable[x] = currentClique;
		}
	}
	
	private void ensureSizeOfCliqueArrays(int size) {
		if (summaryValue == null || summaryValue.length < size) {
			summaryValue = new double [size];
			variableValue = new int [size];
		}
	}
	
	private void applyDynamicProgramming() {
		indexAssigner.clearIndex();
		for (int i=cliques.size()-1; i>=0; i--) {
			cliques.get(i).prepareStructuresForComputation(nonExhaustivelyExploredVariables, marks, indexAssigner);
		}
		ensureSizeOfCliqueArrays(indexAssigner.getIndex());
		for (int i=cliques.size()-1; i>=0; i--) {
			cliques.get(i).applyDynamicProgrammingToClique(red, el, subFunctionsPartition, summaryValue, variableValue);
		}
	}

	private void reconstructOptimalChild(PBSolution child) {
		for (VariableClique clique: cliques) {
			clique.reconstructSolutionInClique(child, red, varProcedence, variableValue);
		}
	}

	public PBSolution recombineInternal(PBSolution blue, PBSolution red) {
	    long initTime = System.nanoTime();
	    this.red = red;
	    this.blue = blue;
	    
	    PBSolution child = new PBSolution(red); //child, copy of red
	    
	    maximumCardinalitySearch();
	    
	    numberOfComponents = 0;
	    groupsOfNonExhaustivelyExploredVariables = 0;
    	nonExhaustivelyExploredVariables.clear();
    	articulationPoints.clear();
	    
	    if (differentSolutions) {
	    	fillIn();
	    	maximumCardinalitySearchBasedOnChordalGraph();
	    	computeSubfunctinsPartition();
		    cliqueTree();
		    cliqueTreeAnalysis();
		    if (debug && ps != null) {
		    	ps.println("Initial label: "+initialLabel);
		    	ps.println("Number of components: "+numberOfComponents);
		    	ps.println(getCliqueTree());
		    }
		    applyDynamicProgramming();
		    reconstructOptimalChild(child);
	    }

		lastRuntime = System.nanoTime() - initTime;
		
		ps.println("* Number of components: "+getNumberOfComponents());
		int logarithmOfExploredSolutions = getLogarithmOfExploredSolutions();
		ps.println("* Logarithm of explored solutions: " + logarithmOfExploredSolutions);
		ps.println("* Full dynastic potential explored: "
				+ (getDifferingVariables() == logarithmOfExploredSolutions));
		ps.println("* Number of articulation points: " + getNumberOfArticulationPoints());
		ps.println("* All articulation points exhaustively explored: "
				+ allArticulationPointsExhaustivelyExplored());
		
		return child;
	}
	
	public PBSolution recombine(PBSolution blue, PBSolution red) {
	    PBSolution solution = recombineInternal(blue, red);
	    if (ps != null) {
	    	ps.println("Recombination time:"+getLastRuntime());
	    }
	    return solution;
	}
	
	/**
	 * This method analyzes the clique tree and put limit to the number of variables for which all the combinations are tested.
	 */
	private void cliqueTreeAnalysis() {
		nonExhaustivelyExploredVariables.clear();
		for (VariableClique clique: cliques) {
			List<Integer> listOfVariables = clique.getVariables().subList(0,clique.getVariablesOfSeparator());
			checkExplorationLimits(listOfVariables);
			
			listOfVariables = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			checkExplorationLimits(listOfVariables);
		}
		
		analysisOfGroupsOfNonExhaustivelyExploredVariables();
		
	}

	protected void analysisOfGroupsOfNonExhaustivelyExploredVariables() {
		List<Set<Integer>> partitionOfNonExploredVariables = new ArrayList<>();
		for (VariableClique clique: cliques) {
			List<Integer> separator = clique.getVariables().subList(0, clique.getVariablesOfSeparator());
			List<Integer> residue = clique.getVariables().subList(clique.getVariablesOfSeparator(), clique.getVariables().size());
			for (List<Integer> listOfVariables : new List[] { separator, residue }) {
				Set<Integer> newComponent = listOfVariables.stream().filter(nonExhaustivelyExploredVariables::contains)
						.collect(Collectors.toSet());
				
				if (newComponent.isEmpty()) {
					continue;
				}
				
				Iterator<Set<Integer>> it = partitionOfNonExploredVariables.iterator();
				Set<Integer> updatedComponent = null;
				while (it.hasNext()) {
					Set<Integer> previousComponent = it.next();
					if (previousComponent.stream().filter(newComponent::contains).findFirst().isPresent()) {
						if (updatedComponent == null) {
							updatedComponent = previousComponent;
							updatedComponent.addAll(newComponent);
						} else {
							updatedComponent.addAll(previousComponent);
							it.remove();
						}
					}
				}
				if (updatedComponent == null) {
					partitionOfNonExploredVariables.add(newComponent);
				}
			}
		}
		groupsOfNonExhaustivelyExploredVariables = partitionOfNonExploredVariables.size();
		
		if (debug) {
			Set<Integer> auxiliar = new HashSet<>();
			for (Set<Integer> component: partitionOfNonExploredVariables) {
				assert auxiliar.stream().filter(component::contains).count()==0;
				auxiliar.addAll(component);
			}
			assert auxiliar.equals(nonExhaustivelyExploredVariables);
		}
		
		for (int i=0; i < partitionOfNonExploredVariables.size(); i++) {
			Set<Integer> component = partitionOfNonExploredVariables.get(i);
			for (int variable : component) {
				marks[variable] = i;
			}
		}
	}

	protected void checkExplorationLimits(List<Integer> listOfVariables) {
		if (listOfVariables.size() > maximumNumberOfVariableToExploreExhaustively) {
			listOfVariables.sort(Comparator.<Integer>comparingInt(variable -> articulationPoints.contains(variable)?0:1)
					.thenComparing(Comparator.<Integer>naturalOrder()));
			nonExhaustivelyExploredVariables.addAll(listOfVariables.subList(maximumNumberOfVariableToExploreExhaustively, listOfVariables.size()));
		}
	}

	public String getCliqueTree() {
		String result = "";
		for (VariableClique clique: cliques) {
			Set<Integer> residue = new HashSet<>();
			residue.addAll(clique.getVariables());
			
			if (clique.getParent() != null) {
				residue.removeAll(clique.getParent().getVariables());
			}
			Set<Integer> separator = new HashSet<>();
			separator.addAll(clique.getVariables());
			separator.removeAll(residue);
			result += "Clique "+clique.getId()+" (parent "+(clique.getParent()!=null?clique.getParent().getId():-1)+"): separator="+separator+ ", residue="+residue+"\n";
		}
		return result;
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

	public Set<Integer> getNonExhaustivelyExploredVariables() {
		return nonExhaustivelyExploredVariables;
	}

	public int getGroupsOfNonExhaustivelyExploredVariables() {
		return groupsOfNonExhaustivelyExploredVariables;
	}
	
	public int getLogarithmOfExploredSolutions() {
		return groupsOfNonExhaustivelyExploredVariables + (getDifferingVariables()-nonExhaustivelyExploredVariables.size());
	}
	
	public int getDifferingVariables() {
		return differentSolutions?(topLabel-initialLabel+1):0;
	}
	
	public int getNumberOfArticulationPoints() {
		return articulationPoints.size();
	}
	
	public boolean allArticulationPointsExhaustivelyExplored() {
		return !nonExhaustivelyExploredVariables.stream().anyMatch(articulationPoints::contains);
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
