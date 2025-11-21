package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import neo.landscape.theory.apps.pseudoboolean.util.ParetoNonDominatedSet.DominanceRelation;


// TODO: this class needs refactoring to separate printing options from archive management
public class ParetoNonDominatedSet implements Iterable<double[]> {

    private String separator = ", ";
    private boolean printSummary = true;

    public enum DominanceRelation {
        DOMINATES, IS_DOMINATED, NON_DOMINATED , EQUAL
    }

    private List<double[]> archive;

    public ParetoNonDominatedSet() {
        archive = new ArrayList<double []>();
    }
    
    public void reportSolutionToArchive(double[] solutionToAdd) {
        reportSolutionToArchive(solutionToAdd, solutionToAdd.length);
    }

    public int size() {
        return archive.size();
    }

    @Override
    public Iterator<double[]> iterator() {
        return archive.iterator();
    }

    public Stream<double[]> stream() {
        return archive.stream();
    }

    public void reportSolutionToArchive(double[] solutionToAdd, int objectives) {
        Iterator<double []> iterator = archive.iterator();
        while(iterator.hasNext()) {
            double [] solutionQuality = iterator.next();
            
            switch (dominanceRelation(solutionToAdd, solutionQuality, objectives)) {
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

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }

    public boolean isPrintSummary() {
        return printSummary;
    }

    public void setPrintSummary(boolean printSummary) {
        this.printSummary = printSummary;
    }

    public String printArchive() {
        StringBuilder result = new StringBuilder();
        if (printSummary) {
            result.append("Archive ("+archive.size()+" solutions):\n");
        }
        for (double [] quality: archive) {
            printVector(result, quality);
        }
        return result.toString();
    }
    
    private void printVector(StringBuilder builder, double [] vector) {
        for (int i = 0; i < vector.length; i++) {
            builder.append(vector[i]);
            if (i < vector.length-1) {
                builder.append(separator);
            }
        }
        builder.append("\n");
    }

    public DominanceRelation dominanceRelation(double[] firstSolution, double[] secondSolution, int objectives) {
        boolean firstBetter=false;
        boolean secondBetter=false;
        
        for (int i = 0; i < objectives; i++) {
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