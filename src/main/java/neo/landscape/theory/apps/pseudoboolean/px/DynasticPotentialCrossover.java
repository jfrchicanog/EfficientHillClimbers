package neo.landscape.theory.apps.pseudoboolean.px;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.UndirectedGraph;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.VariableClique;
import neo.landscape.theory.apps.util.Seeds;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class DynasticPotentialCrossover {
	protected static final int VARIABLE_LIMIT = 1<<29;
	protected EmbeddedLandscape el;
    
	private int [] alpha;
	private int [] alphaInverted;
	private Set<Integer> [] verticesWithNMarks;
	private int [] marks;
	private boolean differentSolutions;
	private int topLabel;
	private int initialLabel;
	// Chordal graph
	private UndirectedGraph chordalGraph;
	// Clique Tree
	private List<VariableClique> cliques;
	
    protected long lastRuntime;

    private int numberOfComponents;
	private int [] fFillin;
	private int [] indexFillin;
	private Set<Integer> [] mSets;
	private VariableClique [] cliqueOfVariable;
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
		verticesWithNMarks = new Set[maxDegree];
		marks = new int [n];
		for (int i=0; i < verticesWithNMarks.length; i++) {
			verticesWithNMarks[i] = new HashSet<>();
		}
		chordalGraph = new UndirectedGraph();
		fFillin = new int[n];
		indexFillin = new int [n];
		mSets = new Set[n];
		for (int i=0; i <n; i++) mSets[i] = new HashSet<>();
		cliques = new ArrayList<>();
		cliqueOfVariable = new VariableClique [n];
		last = new int[n];
		
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
				currentClique.getVariables().add(x);
				
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
	
	public PBSolution recombine(PBSolution blue, PBSolution red) {
	    long initTime = System.nanoTime();
	    this.red = red;
	    this.blue = blue;
	    
	    PBSolution child = new PBSolution(red); //child, copy of red
	    
	    maximumCardinalitySearch();
	    
	    if (differentSolutions) {
	    	fillIn();
		    cliqueTree();
		    if (ps != null) {
		    	ps.println("Initial label: "+initialLabel);
		    	ps.println("Number of components: "+numberOfComponents);
		    	ps.println(getCliqueTree());
		    }
	    }

		lastRuntime = System.nanoTime() - initTime;
		return child;
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
    
}
