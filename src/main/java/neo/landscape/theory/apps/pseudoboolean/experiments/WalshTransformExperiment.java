package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.problems.WalshBasedFunction;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Properties;

public class WalshTransformExperiment implements Process {

    protected EmbeddedLandscape pbf;
    protected boolean showEvaluation = false;
    protected long seed;


    @Override
    public void execute(String [] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }

        int argN = 0;
        if (args[argN].equals("-e")) {
            showEvaluation = true;
            argN++;
        }

        if ("nk".equals(args[argN])) {
            args= Arrays.copyOfRange(args, argN+1, args.length);
            pbf = configureNKInstance(args);
        } else if ("maxsat".equals(args[argN])) {
            args= Arrays.copyOfRange(args, argN+1, args.length);
            pbf = configureMaxsatInstance(args);
        } else if ("walsh".equals(args[argN])) {
            args= Arrays.copyOfRange(args, argN+1, args.length);
            pbf = configureWalshBasedInstance(args);
        }

        if (pbf == null) {
            System.out.println(getInvocationInfo());
            return;
        }

        WalshCoefficients transform = WalshTransform.transform(pbf);
        reportWalshTransform(transform);
        if (showEvaluation) {
            reportEvaluation(pbf);
        }

        if (pbf instanceof NKLandscapes) {
            reportNKInstanceToStandardOutput();
        }

    }

    private void reportEvaluation(EmbeddedLandscape pbf) {
        long max=1 << pbf.getN();
        int n = pbf.getN();
        System.out.println("Evaluation");
        System.out.println("==========");
        for (int x=0; x < max; x++) {
            PBSolution solution = PBSolution.readFromInt(n, x);
            System.out.println(solution.toString()+": "+pbf.evaluate(solution));
        }
    }

    private EmbeddedLandscape configureWalshBasedInstance(String[] args) {
        String instance = args[0];
        Properties prop = new Properties();
        prop.setProperty(WalshBasedFunction.INSTANCE_STRING, instance);
        WalshBasedFunction walsh = new WalshBasedFunction();
        walsh.setConfiguration(prop);
        return walsh;
    }

    private void reportWalshTransform(WalshCoefficients transform) {
        System.out.println("Walsh Transform");
        System.out.println("===============");
        System.out.println(transform);
        System.out.println("===============");
    }

    private void reportNKInstanceToStandardOutput() {
        ((NKLandscapes)pbf).writeTo(new OutputStreamWriter(System.out));
    }

    private EmbeddedLandscape configureNKInstance(String[] args) {
        String n = args[0];
        String k = args[1];
        String q = args[2];
        String circular = args[3];
        if (args.length > 4) {
            seed = Long.parseLong(args[4]);
        } else {
            seed = Seeds.getSeed();
        }

        return createNKInstance(n, k, q, circular);
    }

    private EmbeddedLandscape createNKInstance(String n, String k, String q, String circular) {
        NKLandscapes pbf = new NKLandscapes();
        Properties prop = new Properties();
        prop.setProperty(NKLandscapes.N_STRING, n);
        prop.setProperty(NKLandscapes.K_STRING, k);

        if (!q.equals("-")) {
            prop.setProperty(NKLandscapes.Q_STRING, q);
        }

        if (circular.equals("y")) {
            prop.setProperty(NKLandscapes.CIRCULAR_STRING, "yes");
        }

        pbf.setSeed(seed);
        pbf.setConfiguration(prop);

        return pbf;
    }

    private EmbeddedLandscape configureMaxsatInstance(String [] args) {
        String instance = args[0];
        Properties prop = new Properties();
        prop.setProperty(MAXSAT.INSTANCE_STRING, instance);
        MAXSAT maxsat = new MAXSAT();
        maxsat.setConfiguration(prop);
        return maxsat;
    }


    @Override
    public String getDescription() {
        return "Computes the Walsh Transform of an embedded landscape";
    }

    @Override
    public String getID() {
        return "walsh-transform";
    }

    @Override
    public String getInvocationInfo() {
        return "Arguments: " + getID() + " [-e] (nk <n> <k> <q> <circular> [<seed>] | maxsat <instance> | walsh <instance>)";
    }
}
