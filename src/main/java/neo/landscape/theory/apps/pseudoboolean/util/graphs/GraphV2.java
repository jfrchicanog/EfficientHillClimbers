package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.Iterator;

public interface GraphV2 {
    public Iterator<Integer> adjacentVertices(int vertex);
    public int numberOfVertices();
}