package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class ArticulationPointsCalculator {
    static public interface Graph {
        public int numberOfAdjacentVertices(int vertex);
        public int adjacentVertexNumber(int vertex, int index);
        public int numberOfVertices();
    }
    
    static private class VertexIndex {
        private int vertex;
        private int index;
        
        VertexIndex(int v, int i) {
            vertex = v;
            index = i;
        }
        
        static VertexIndex newVertexIndex(int v, int i) {
            return new VertexIndex(v, i);
        }
    }
    
    static private class Edge {
        private int tail;
        private int head;
        
        Edge(int tail, int head) {
            this.tail =tail;
            this.head = head;
        }
        
        static Edge newEdge(int tail, int head) {
            return new Edge(tail, head);
        }
    }
    
    private TwoStatesIntegerSet explored;
    private int low[];
    private int time[];
    private int parent[];
    private int globalTime;
    private TwoStatesIntegerSet articulationPoints;
    private Stack<VertexIndex> stack;
    private Stack<Edge> edgeStack;
    private Set<Set<Integer>> biconnectedComponents;

    public void computeArticulationPoints(Graph graph) {
        if (graph.numberOfVertices() <= 0) {
            return;
        }

        initializeDataStructures(graph);

        while (explored.hasMoreUnexplored()) {
            int root = explored.getNextUnexplored();

            parent[root] = -1;
            int rootChildren = 0;
            stack.add(VertexIndex.newVertexIndex(root, 0));
            while (!stack.isEmpty()) {
                VertexIndex vi = stack.pop();
                int v = vi.vertex;
                int i = vi.index;
                if (i == 0) {
                    explored.explored(v);
                    time[v] = globalTime++;
                    low[v] = time[v];
                }
                if (i < graph.numberOfAdjacentVertices(v)) {
                    stack.add(VertexIndex.newVertexIndex(v, i+1));
                    int w = graph.adjacentVertexNumber(v, i);
                    if (!explored.isExplored(w)) { 
                        if (parent[v] < 0) {
                            rootChildren++;
                        }
                        edgeStack.add(Edge.newEdge(v,  w));
                        stack.add(VertexIndex.newVertexIndex(w, 0));
                        parent[w] = v;

                    } else if (time[w] < time[v] && w != parent[v]) {
                        edgeStack.add(Edge.newEdge(v, w));
                        low[v] = Math.min(low[v], time[w]);
                    }
                } else if (!stack.isEmpty()){
                    int w = v;
                    vi = stack.peek();
                    v = vi.vertex;
                    i = vi.index;
                    low[v] = Math.min(low[v],low[w]);
                    if (low[w] >= time[v]) {
                        if (parent[v] >= 0) {
                            articulationPoints.explored(v);
                        }
                        if (!edgeStack.isEmpty()) {
                            Set<Integer> component = new HashSet<>();
                            while (time[edgeStack.peek().tail] >= time[w]) {
                                Edge e = edgeStack.pop();
                                component.add(e.head);
                                component.add(e.tail);
                            }
                            Edge e = edgeStack.pop();
                            component.add(e.head);
                            component.add(e.tail);
                            biconnectedComponents.add(component);
                        }
                    }
                }
            }

            if (rootChildren >= 2) {
                articulationPoints.explored(root);
            }
        }
    }

    private void initializeDataStructures(Graph graph) {
        int n = graph.numberOfVertices();
        explored = new TwoStatesISArrayImpl(n);
        articulationPoints = new TwoStatesISArrayImpl(n);
        explored.reset();
        articulationPoints.reset();
        
        low = new int[n];
        time = new int [n];
        parent = new int [n];
        globalTime = 0;
        stack = new Stack<>();
        edgeStack = new Stack<>();
        biconnectedComponents = new HashSet<>();
    }
    
    public Set<Set<Integer>> getBiconnectedComponents() {
        return biconnectedComponents;
    }
    
    public Set<Integer> getArticulationPoints() {
        return IntStream.range(0,articulationPoints.getNumberOfElements())
        .filter(i -> articulationPoints.isExplored(i))
        .boxed()
        .collect(Collectors.toSet());
    }

}
