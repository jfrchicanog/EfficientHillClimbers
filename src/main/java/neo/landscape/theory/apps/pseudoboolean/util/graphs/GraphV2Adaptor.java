package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.Iterator;

public class GraphV2Adaptor implements GraphV2{
    private Graph graph;
    public GraphV2Adaptor(Graph g) {
        this.graph=g;
    }
    @Override
    public Iterator<Integer> adjacentVertices(int vertex) {
        return new Iterator<Integer> () {
            int i=0;
            @Override
            public boolean hasNext() {
                return i < graph.numberOfAdjacentVertices(vertex);
            }

            @Override
            public Integer next() {
                return graph.adjacentVertexNumber(vertex, i++);
            }
            
        };
    }
    @Override
    public int numberOfVertices() {
        return graph.numberOfVertices();
    }
}