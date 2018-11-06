package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TriangularizationAlgorithm {
	
	private GraphV2 graph;
	private Set<Integer> [] verticesWithNMarks;
	private int [] marks;
	private int [] alpha;
	private int [] alphaInverted;
	// Chordal graph after fillIn
	private Map<Integer, List<Integer>> chordalGraph;
	// Clique Tree
	private Map<Integer, Set<Integer>> cliques;
	private List<Integer> parentClique;
	
	
	
	public TriangularizationAlgorithm(GraphV2 graph) {
		this.graph = graph;
		verticesWithNMarks = new Set[graph.numberOfVertices()];
		marks = new int [graph.numberOfVertices()];
		alpha = new int [graph.numberOfVertices()];
		alphaInverted = new int [graph.numberOfVertices()+1];
	}
	
	public void maximumCardinalitySearch() {
		int n = graph.numberOfVertices();
		for (int vertex=0; vertex < n; vertex++) {
			verticesWithNMarks[vertex] = new HashSet<>();
			marks[vertex] = 0;
			verticesWithNMarks[0].add(vertex);
		}
		int i=n;
		int j=0;
		while (i>=1) {
			int vertex = verticesWithNMarks[j].iterator().next();
			verticesWithNMarks[j].remove(vertex);
			alpha[vertex] = i;
			alphaInverted[i] = vertex;
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
		clearChordalGraph();
		int n = graph.numberOfVertices();
		int [] f = new int[n];
		int [] index = new int [n];
		for (int i=1; i <= n; i++) {
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
						addNodeToChordalGraph(x, w);
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
		
		for (int i=0; i <n; i++) mSets[i] = new HashSet<>();
		
		int [] mark = new int[n]; 
		int [] clique = new int [n];
		int previousMark = -1;
		int [] last =  new int[n];
		int j=0;
		
		for (int i=0; i < n; i++) last[i] = -1;
		
		parentClique = new ArrayList<>(); 
		
		Set<Integer> currentClique=new HashSet<>();
		cliques = new HashMap<>();
		cliques.put(j, currentClique);
		
		parentClique.add(-1); // First clique, without parent
		
		for (int i=n; i>=1; i--) {
			int x = alphaInverted[i];
			if (mark[x] <= previousMark) {
				j++;
				currentClique=new HashSet<>();
				cliques.put(j, currentClique);
				
				currentClique.addAll(mSets[x]);
				currentClique.add(x);
				
				if (last[x] < 0) {
					parentClique.add(-1);
				} else {
					parentClique.add(clique[last[x]]);
				}
			} else {
				currentClique.add(x);
			}
			for (Integer y: chordalGraph.get(x)) {
				mSets[y].add(x);
				mark[y]++;
				last[y] = x;
			}
			previousMark = mark[x];
			clique[x] = j;
		}
	}
	
	
	
	private void clearChordalGraph() {
		if (chordalGraph==null) {
			chordalGraph = new HashMap<>();
		}
		chordalGraph.clear();
	}
	
	private void addNodeToChordalGraph(int from, int to) {
		addArcToChordalGraph(from, to);
		addArcToChordalGraph(to, from);
	}

	protected void addArcToChordalGraph(int from, int to) {
		List<Integer> adjacentVertices = chordalGraph.get(from);
		if (adjacentVertices==null) {
			adjacentVertices = new ArrayList<>();
			chordalGraph.put(from, adjacentVertices);
		}
		adjacentVertices.add(to);
	}
	
	public int [] getAlpha() {
		return alpha;
	}

	public int[] getAlphaInverted() {
		return alphaInverted;
	}

	public Map<Integer, Set<Integer>> getCliques() {
		return cliques;
	}

	public List<Integer> getParentClique() {
		return parentClique;
	}
	
	public String getCliqueTree() {
		String result = "";
		for (int i=0; i < parentClique.size(); i++) {
			Integer parent = parentClique.get(i);
			Set<Integer> residue = new HashSet<>();
			residue.addAll(cliques.get(i));
			if (parent >= 0) {
				residue.removeAll(cliques.get(parent));
			}
			Set<Integer> separator = new HashSet<>();
			separator.addAll(cliques.get(i));
			separator.removeAll(residue);
			result += "Clique "+i+" (parent "+parent+"): separator="+separator+ ", residue="+residue+"\n";
		}
		return result;
	}
	
	

}
