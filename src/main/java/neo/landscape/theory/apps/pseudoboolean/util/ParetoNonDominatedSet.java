package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet.DominanceRelation;

public class ParetoNonDominatedSet {
    public enum DominanceRelation {
        DOMINATES, IS_DOMINATED, NON_DOMINATED , EQUAL
    }

    private List<double[]> archive;

    public ParetoNonDominatedSet() {
        archive = new ArrayList<double []>();
    }

    public void reportSolutionToArchive(double[] solutionToAdd) {
        Iterator<double []> iterator = archive.iterator();
        while(iterator.hasNext()) {
            double [] solutionQuality = iterator.next();
            
            switch (dominanceRelation(solutionToAdd, solutionQuality)) {
            case DOMINATES:
                iterator.remove();
                break;
            case IS_DOMINATED:
            case EQUAL:
                return;
            }
        }
        archive.add(solutionToAdd.clone());
    }

    public String printArchive() {
        StringBuilder result = new StringBuilder("Archive ("+archive.size()+" solutions):\n");
        for (double [] quality: archive) {
            printVector(result, quality);
        }
        return result.toString();
        
    }
    
    private void printVector(StringBuilder builder, double [] vector) {
        for (int i = 0; i < vector.length; i++) {
            builder.append(vector[i]);
            if (i < vector.length-1) {
                builder.append(", ");
            }
        }
        builder.append("\n");
    }

    public DominanceRelation dominanceRelation(double[] firstSolution, double[] secondSolution) {
        boolean firstBetter=false;
        boolean secondBetter=false;
        
        for (int i = 0; i < firstSolution.length; i++) {
            firstBetter |= (firstSolution[i] > secondSolution[i]);
            secondBetter |= (firstSolution[i] < secondSolution[i]);
        }
        
        if (firstBetter && !secondBetter) {
            return DominanceRelation.DOMINATES;
        } else if (secondBetter && !firstBetter) {
            return DominanceRelation.IS_DOMINATED;
        } else if (!secondBetter && !firstBetter) {
            return DominanceRelation.EQUAL;
        } else {
            return DominanceRelation.NON_DOMINATED;
        }
    }
}