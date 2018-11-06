package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import neo.landscape.theory.apps.pseudoboolean.util.graphs.Graph;
import neo.landscape.theory.apps.pseudoboolean.util.graphs.SampleGraph;

public class APCTestCase {
    protected Integer[][] graph;
    protected Integer[] articulationPoints;
    protected Integer[][] biconnectedComponents;

    public APCTestCase() {
    }

    protected Set<Integer> getArticulationPoints() {
        return Arrays.stream(articulationPoints)
                .collect(Collectors.toSet());
    }

    Graph getGraph() {
        return new SampleGraph (graph);
    }

    protected Set<Set<Integer>> getBiconnectedComponents() {
        return Arrays.stream(biconnectedComponents)
        .map(a -> Arrays.stream(a).collect(Collectors.toSet()))
        .collect(Collectors.toSet());
    }
}