package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.util.ArrayList;
import java.util.List;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimber;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallPBMove;

public class GoldmanEnumeration extends LocalOptimaExperiment {
    
    protected List<RBallPBMove> [] moveBin; 

    @Override
    public String getDescription() {
        return "This experiment computes the All the Local Optima of the search "
        + "space using the Goldman algorithm";
    }

    @Override
    public String getID() {
        return "lo-goldman";
    }

    @Override
    protected List<PBSolution> findLocalOptima() {
        int n = pbf.getN();
        int index = n-1;
        PBSolution solution = rball.getSolution();
        initializeMoveBin();
        
        while (index < n) {
            index = findNextIndex(index);
            if (index < 0) {
                addLocalOptima(rball, pbf);
                index = 0;
            }
            while (index < n && solution.getBit(index) == 1) {
                rball.moveOneBit(index);
                index++;
            }
            if (index < n) {
                rball.moveOneBit(index);
            }
        }
        return localOptima;
    }

    protected int findNextIndex(int index) {
        while (index >= 0) {
            for (RBallPBMove move : moveBin[index]) {
                if (move.improvement > 0) {
                    return index;
                }
            }
            index--;
        }
        return index;
    }
    
    protected void initializeMoveBin() {
        moveBin = new List[pbf.getN()];
        for (int i = 0; i < moveBin.length; i++) {
            moveBin[i] = new ArrayList<RBallPBMove>();
        }
        for (RBallPBMove move: rball.iterateOverMoves()) {
            int min = Integer.MAX_VALUE;
            for (int subfn : rballfio.subFunctionsAffected(move.flipVariables)) {
                int [] mask = pbf.getMasks()[subfn];
                for (int var: mask) {
                    if (var < min) {
                        min = var;
                    }
                }
            }
            
            moveBin[min].add(move);
            System.out.println("Move "+move+" in "+min);
        }

    }

    @Override
    protected void prepareRBallExplorationAlgorithm() {
        rballfio = (RBallEfficientHillClimberForInstanceOf) new RBallEfficientHillClimber(
                r).initialize(pbf);
        PBSolution pbs = new PBSolution(pbf.getN());
        
        
        rball = rballfio.initialize(pbs);
        rball.setSeed(seed);
    }

}
