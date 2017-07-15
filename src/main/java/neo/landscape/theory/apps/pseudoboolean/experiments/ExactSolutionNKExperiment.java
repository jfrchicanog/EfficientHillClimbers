package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod.SolutionQuality;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.CompleteEnumeration;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.CompleteEnumerationBigDecimal;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProg;
import neo.landscape.theory.apps.pseudoboolean.exactsolvers.NKLandscapesCircularDynProgBigDecimal;
import neo.landscape.theory.apps.pseudoboolean.parsers.NKLandscapesTinosReader;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

public class ExactSolutionNKExperiment implements Process {

    @Override
    public String getDescription() {
        return "Computes the exact solution of an NK-landscape using several algorithms";
    }

    @Override
    public String getID() {
        return "exact-nk";
    }

    @Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() 
                + " (enum|enumbd|dynp|dynpbd) (<n> <k> <q> <c> [<seed>] | <file (in Tinos format)>) /* instances must follow the adjacent model */";
    }

    @Override
    public void execute(String[] arguments) {

        if (arguments.length < 2) {
            System.out.println(getInvocationInfo());
            return;
        }

        String algorithm = arguments[0];
        ExactSolutionMethod<? super NKLandscapes> es = createSolutionMethod(algorithm);
        
        NKLandscapes instance = getNKLandscapeInstance(Arrays.copyOfRange(arguments, 1, arguments.length));

        if (instance == null) {
            System.err.println("No instance provided");
            return;
        }
        
        SolutionQuality<? super NKLandscapes> solutionQuality = es.solveProblem(instance);

        System.out.println("Optimal solution: " + solutionQuality.solution);
        System.out.println("Optimal fitness: " + solutionQuality.quality);

    }
    
    private NKLandscapes getNKLandscapeInstance(String[] instanceParameters) {
        NKLandscapes instance= tryGeneratingRandomInstance(instanceParameters);
        if (instance == null) {
            instance = tryReadingInstanceFromFile(instanceParameters);
        }
        return instance;
    }

    private NKLandscapes tryGeneratingRandomInstance(String[] instanceParameters) {
        try {
            return generateRandomInstance(instanceParameters);
        } catch (RuntimeException e) {
            return null;
        }
    }
    
    private NKLandscapes tryReadingInstanceFromFile(String[] instanceParameters) {
        File file = new File (instanceParameters[0]);
        if (file.exists()) {
            return tryReadingFile(file);
        }
        return null;
    }

    private NKLandscapes tryReadingFile(File file) {
        try {
            Readable readable = new FileReader(file);
            NKLandscapes instance = new NKLandscapesTinosReader().readInstance(readable);
            return instance;
        } catch (Exception e){
            return null;
        }
    }

    private NKLandscapes generateRandomInstance(String[] instanceParameters) {
        String n = instanceParameters[0];
        String k = instanceParameters[1];
        String q = instanceParameters[2];
        String circular = instanceParameters[3];
        long seed = 0;
        if (instanceParameters.length >= 5) {
            seed = Long.parseLong(instanceParameters[4]);
        } else {
            seed = Seeds.getSeed();
        }
        
        if (circular.equals("y")) {
            circular = "yes";
        }

        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);
        prop.setProperty(NKLandscapes.CIRCULAR_STRING, circular);
        if (!q.equals("-")) {
            prop.setProperty(NKLandscapes.Q_STRING, q);
        }

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);
        return pbf;
    }

    private ExactSolutionMethod<? super NKLandscapes> createSolutionMethod(
            String algorithm) {
        if (algorithm.equals("enum")) {
            return new CompleteEnumeration<NKLandscapes>();
        } else if (algorithm.equals("enumbd")) {
            return new CompleteEnumerationBigDecimal<NKLandscapes>();
        } else if (algorithm.equals("dynp")) {
            return new NKLandscapesCircularDynProg();
        } else if (algorithm.equals("dynpbd")) {
            return new NKLandscapesCircularDynProgBigDecimal();
        } else {
            throw new RuntimeException("Unknown algorithm: " + algorithm);
        }
    }


}
