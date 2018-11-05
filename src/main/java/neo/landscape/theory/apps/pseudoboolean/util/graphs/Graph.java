package neo.landscape.theory.apps.pseudoboolean.util.graphs;

public interface Graph {
    public int numberOfAdjacentVertices(int vertex);
    public int adjacentVertexNumber(int vertex, int index);
    public int numberOfVertices();
}