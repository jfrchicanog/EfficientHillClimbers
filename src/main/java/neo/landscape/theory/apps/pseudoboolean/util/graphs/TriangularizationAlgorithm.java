package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class TriangularizationAlgorithm {
	
	private GraphV2 graph;
	
	private int [] alpha;
	private int [] alphaInverted;
	private int topLabel;
	private int initialLabel;
	// Chordal graph
	private UndirectedGraph chordalGraph;
	// Clique Tree
	private List<VariableClique> cliques;
	
	
	
	public TriangularizationAlgorithm(GraphV2 graph) {
		this.graph = graph;
		int n = graph.numberOfVertices();
		alpha = new int [n];
		topLabel = n-1;
		alphaInverted = new int [topLabel+1];
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
		chordalGraph = new UndirectedGraph();
		int n = graph.numberOfVertices();
		int [] f = new int[n];
		int [] index = new int [n];
		for (int i=initialLabel; i <= topLabel; i++) {
			int w = alphaInverted[i];
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
		
		Set<Integer> [] mSets = new Set[n];
		cliques = new ArrayList<>();
		
		for (int i=0; i <n; i++) mSets[i] = new HashSet<>();
		
		int [] mark = new int[n]; 
		VariableClique [] clique = new VariableClique [n];
		int previousMark = -1;
		int [] last =  new int[n];
		int j=0;
		
		for (int vertex: IntStream.range(0, n).toArray()) last[vertex] = -1;
		
		VariableClique currentClique=new VariableClique(j);
		cliques.add(currentClique);
		
		for (int i=topLabel; i>=initialLabel; i--) {
			int x = alphaInverted[i];
			if (mark[x] <= previousMark) {
				j++;
				currentClique=new VariableClique(j);
				cliques.add(currentClique);
				
				currentClique.getVariables().addAll(mSets[x]);
				currentClique.markSeparator();
				currentClique.getVariables().add(x);

				if (last[x] >= 0) {
					currentClique.setParent(clique[last[x]]);
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
			clique[x] = currentClique;
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
		return cliques;
	}

	public String getCliqueTree() {
		String result = "";
		for (VariableClique clique: cliques) {
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
