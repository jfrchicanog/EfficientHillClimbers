package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import neo.landscape.theory.apps.pseudoboolean.experiments.ExactSolutionNKExperiment;

public class ExactSolutionNK {

    public static void main (String [] arguments) {
        neo.landscape.theory.apps.util.Process process = new ExactSolutionNKExperiment();
        System.out.println("WARNING: Don't use "+process.getID()+ " as first argument");
        process.execute(arguments);
    }
    
}
