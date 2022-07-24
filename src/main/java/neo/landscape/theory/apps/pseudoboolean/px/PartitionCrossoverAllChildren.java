package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;

public class PartitionCrossoverAllChildren extends PartitionCrossover {
    
    public PartitionCrossoverAllChildren(EmbeddedLandscape el) {
        super(el);
    }
    
    public List<PBSolution> getAllChildren(PBSolution red, PBSolution blue) {
        List<Set<Integer>> partition = computePartition(blue, red);
        // Only work for at most 63 components 
        long numberOfChildren = 1<<partition.size();
        List<PBSolution> result = new ArrayList<>();
        for (long child=0; child < numberOfChildren; child++) {
            PBSolution newChild = new PBSolution (red);
            for (int component = 0; component < partition.size(); component++) {
                Set<Integer> vars = partition.get(component);
                if (((child >>> component) & 0x1L) != 0) {
                    for (int v : vars) {
                        newChild.setBit(v, blue.getBit(v));
                    }
                }
            }
            result.add(newChild);
        }
        
        return result;
    }

}
