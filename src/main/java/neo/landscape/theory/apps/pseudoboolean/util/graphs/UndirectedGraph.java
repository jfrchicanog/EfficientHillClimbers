package neo.landscape.theory.apps.pseudoboolean.util.graphs;

public interface UndirectedGraph {

	void clearGraph();

	void addNodeToGraph(int node);

	void addEdgeToGraph(int from, int to);

	Iterable<Integer> getAdjacent(Integer vertex);

	Iterable<Integer> getNodes();

}