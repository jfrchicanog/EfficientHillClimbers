package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UndirectedGraph {
	private Map<Integer, List<Integer>> adjacencyMatrix;

	public UndirectedGraph() {
		clearGraph();
	}

	public void clearGraph() {
		if (adjacencyMatrix==null) {
			adjacencyMatrix = new HashMap<>();
		}
		adjacencyMatrix.clear();
	}

	public void addNodeToGraph(int node) {
		List<Integer> adjacentVertices = adjacencyMatrix.get(node);
		if (adjacentVertices==null) {
			adjacentVertices = new ArrayList<>();
			adjacencyMatrix.put(node, adjacentVertices);
		}
	}
	
	public void addEdgeToGraph(int from, int to) {
		addArcToGraph(from, to);
		addArcToGraph(to, from);
	}

	private void addArcToGraph(int from, int to) {
		List<Integer> adjacentVertices = adjacencyMatrix.get(from);
		if (adjacentVertices==null) {
			adjacentVertices = new ArrayList<>();
			adjacencyMatrix.put(from, adjacentVertices);
		}
		adjacentVertices.add(to);
	}
	
	public Iterable<Integer> getAdjacent(Integer vertex) {
		if (adjacencyMatrix.get(vertex)==null) {
			throw new IllegalArgumentException("This vertex is not in the graph: "+vertex);
		} else {
			return adjacencyMatrix.get(vertex);
		}
	}
	
	public Iterable<Integer> getNodes() {
		return adjacencyMatrix.keySet();
	}
}