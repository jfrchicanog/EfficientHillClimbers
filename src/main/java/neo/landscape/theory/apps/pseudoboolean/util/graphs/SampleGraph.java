package neo.landscape.theory.apps.pseudoboolean.util.graphs;


public class SampleGraph implements Graph {
    private Integer [][] graph;
    
    /**
     * 
     * @param graph is the graph in with the representation of list of adjacencies.
     */
    public SampleGraph (Integer [][] graph) {
        this.graph = graph;
    }
    
    @Override
    public int numberOfAdjacentVertices(int vertex) {
        return graph[vertex].length;
    }

    @Override
    public int adjacentVertexNumber(int vertex, int index) {
        return graph[vertex][index];
    }

    @Override
    public int numberOfVertices() {
        return graph.length;
    }
}