package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.pseudoboolean.px.CliqueManagement;
import neo.landscape.theory.apps.pseudoboolean.px.CliqueManagementBasicImplementation;
import neo.landscape.theory.apps.pseudoboolean.px.CliqueManagementFactory;

public class TriangularizationAlgorithm {
	
	private GraphV2 graph;
	
	private int [] alpha;
	private int [] alphaInverted;
	private int topLabel;
	private int initialLabel;
	// Chordal graph
	private UndirectedGraph chordalGraph;
	// Clique Tree
	private CliqueManagement cliqueManagement;
	private CliqueManagementFactory cmFactory = CliqueManagementBasicImplementation.FACTORY;
	private UndirectedGraphFactory ugFactory;
	
	
	
	public TriangularizationAlgorithm(GraphV2 graph, UndirectedGraphFactory ugFactory) {
		this.graph = graph;
		int n = graph.numberOfVertices();
		alpha = new int [n];
		topLabel = n-1;
		alphaInverted = new int [topLabel+1];
		this.ugFactory=ugFactory;
		cliqueManagement = cmFactory.createCliqueManagement();
	}
	
	public TriangularizationAlgorithm(GraphV2 graph) {
		this(graph, MapBasedUndirectedGraph.FACTORY);
	}
	
	public void maximumCardinalitySearch() {
		int n = graph.numberOfVertices();
		int maxDegree = n;
		
		Set<Integer> [] verticesWithNMarks = new Set[maxDegree];
		int [] marks = new int [n];
		
		for (int i=0; i < verticesWithNMarks.length; i++) {
			verticesWithNMarks[i] = new HashSet<>();
		}
		
		for (int vertex: IntStream.range(0, n).toArray()) {
			marks[vertex] = 0;
			verticesWithNMarks[0].add(vertex);
		}
		
		if (n==0) return;
		
		int i=topLabel;
		int j=0;
		while (j>=0) {
			int vertex = verticesWithNMarks[j].iterator().next();
			verticesWithNMarks[j].remove(vertex);
			alpha[vertex] = i;
			alphaInverted[i] = vertex;
			initialLabel=i;
			marks[vertex] = -1;
			
			Iterator<Integer> it = graph.adjacentVertices(vertex);
			while (it.hasNext()) {
				int w = it.next();
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
	
	public void fillIn() {
		int n = graph.numberOfVertices();
		chordalGraph = ugFactory.createGraph(graph.numberOfVertices());
		int [] f = new int[n];
		int [] index = new int [n];
		for (int i=initialLabel; i <= topLabel; i++) {
			int w = alphaInverted[i];
			chordalGraph.addNodeToGraph(w);
			f[w] = w;
			index[w] = i;
			Iterator<Integer> it = graph.adjacentVertices(w);
			while (it.hasNext()) {
				int v = it.next();
				if (alpha[v] < i) {
					int x=v;
					while (index[x] < i) {
						index[x] = i;
						chordalGraph.addEdgeToGraph(x, w);
						x = f[x];
					}
					if (f[x]==x) {
						f[x] = w;
					}
				}
			}
		}
	}
	
	public void cliqueTree() {
		int n = graph.numberOfVertices();
		
		List<Integer> [] mSets = new List[n];
		
		for (int i=0; i <n; i++) mSets[i] = new ArrayList<>();
		
		int [] mark = new int[n]; 
		int [] clique = new int [n];
		int previousMark = -1;
		int [] last =  new int[n];
		
		for (int vertex: IntStream.range(0, n).toArray()) last[vertex] = -1;
		
		VariableClique currentClique=cliqueManagement.addNewVariableClique();
		
		for (int i=topLabel; i>=initialLabel; i--) {
			int x = alphaInverted[i];
			if (mark[x] <= previousMark) {
				currentClique=cliqueManagement.addNewVariableClique();
				
				currentClique.addAllVariables(mSets[x]);
				currentClique.markSeparator();
				currentClique.addVariable(x);

				if (last[x] >= 0) {
					cliqueManagement.setVariableCliqueParent(currentClique.getId(), clique[last[x]]);
				}
			} else {
				currentClique.getVariables().add(x);
			}
			for (Integer y: chordalGraph.getAdjacent(x)) {
				mSets[y].add(x);
				mark[y]++;
				last[y] = x;
			}
			previousMark = mark[x];
			clique[x] = currentClique.getId();
		}
	}
	
	public void updateLabellingBasedOnChordalGraph() {
		// TODO
		
	}
	
	public int [] getAlpha() {
		return alpha;
	}

	public int[] getAlphaInverted() {
		return alphaInverted;
	}

	public List<VariableClique> getCliques() {
		return cliqueManagement.getCliques();
	}

	public String getCliqueTree() {
		String result = "";
		for (VariableClique clique: cliqueManagement.getCliques()) {
			List<Integer> separator = clique.getVariables().subList(0, clique.getVariablesOfSeparator());
			List<Integer> residue = clique.getVariables().subList(clique.getVariablesOfSeparator(),clique.getVariables().size());
			
			result += "Clique "+clique.getId()
					+" (parent "+(clique.getParent()!=null?clique.getParent().getId():-1)
					+"): separator="+separator
					+ ", residue="
					+residue+"\n";
		}
		return result;
	}

	public int getInitialLabel() {
		return initialLabel;
	}
	
	

}
