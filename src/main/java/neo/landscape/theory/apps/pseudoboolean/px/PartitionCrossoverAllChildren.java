package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.*;
import java.util.stream.StreamSupport;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class PartitionCrossoverAllChildren extends PartitionCrossover {
    
    public PartitionCrossoverAllChildren(EmbeddedLandscape el) {
        super(el);
    }
    
    public Lattice getAllChildren(PBSolution red, PBSolution blue) {
        List<Set<Integer>> partition = new ArrayList<>();

        bfsSet.reset();
        for (Integer node = nextNodeInReducedGraph(blue, red); node != null; node = nextNodeInReducedGraph(blue, red)) {
            PartitionComponent component = bfs(node, blue, red);
            Set<Integer> set = new HashSet<>();
            for (int var: component) {
                set.add(var);
            }
            partition.add(set);
        }

        return new Lattice(red, partition);
    }

}
