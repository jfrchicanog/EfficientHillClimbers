package neo.landscape.theory.apps.pseudoboolean.experiments;

import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberForInstanceOf;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.MAXSAT;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshCoefficients;
import neo.landscape.theory.apps.pseudoboolean.util.walsh.WalshTransform;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.Seeds;

import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Properties;

public class WalshTransformExperiment implements Process {

    protected EmbeddedLandscape pbf;
    protected int r;
    protected RBallEfficientHillClimberSnapshot rball;
    protected RBallEfficientHillClimberForInstanceOf rballfio;
    protected long seed;
    protected String prefix="";

    @Override
    public void execute(String [] args) {
        if (args.length < 1) {
            System.out.println(getInvocationInfo());
            return;
        }

        if ("nk".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            pbf = configureNKInstance(args);
        } else if ("maxsat".equals(args[0])) {
            args= Arrays.copyOfRange(args, 1, args.length);
            pbf = configureMaxsatInstance(args);
        }

        if (pbf == null) {
            System.out.println(getInvocationInfo());
            return;
        }

        WalshCoefficients transform = WalshTransform.transform(pbf);
        reportWalshTransform(transform);

        if (pbf instanceof NKLandscapes) {
            reportNKInstanceToStandardOutput();
        }

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
        r = Integer.parseInt(args[4]);
        seed = 0;
        if (args.length >= 6) {
            seed = Long.parseLong(args[5]);
        } else {
            seed = Seeds.getSeed();
        }

        if (args.length >= 7) {
            prefix = args[6];
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
        r = Integer.parseInt(args[1]);
        seed = 0;
        if (args.length >= 3) {
            seed = Long.parseLong(args[2]);
        } else {
            seed = Seeds.getSeed();
        }

        if (args.length >= 4) {
            prefix = args[3];
        }

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
        return "Arguments: " + getID() + " (nk <n> <k> <q> <circular> <r> | maxsat <instance> <r>)";
    }
}
