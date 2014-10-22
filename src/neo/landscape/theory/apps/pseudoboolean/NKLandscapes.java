package neo.landscape.theory.apps.pseudoboolean;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Properties;

import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.util.Seeds;

public class NKLandscapes extends EmbeddedLandscape implements
		KBoundedEpistasisPBF {

	public static final String N_STRING = "n";
	public static final String K_STRING = "k";
	public static final String Q_STRING = "q";
	public static final String CIRCULAR_STRING = "circular";
	public static final String FORCE_NK = ".";

	private double[][] subfunctions;
	private int q;
	private boolean circular;
	protected int k;

	@Override
	public void setConfiguration(Properties prop) {

		n = Integer.parseInt(prop.getProperty(N_STRING));
		k = Integer.parseInt(prop.getProperty(K_STRING)) + 1;
		m = n;

		int twoToK = 1 << k;
		q = twoToK;

		if (prop.getProperty(Q_STRING) != null) {
			if (prop.getProperty(Q_STRING).equals(FORCE_NK)) {
				// Generate an NK-landscape instead of NKq-landscape
				q = -1;
			} else {
				q = Integer.parseInt(prop.getProperty(Q_STRING));
			}
		}

		circular = false;
		if (prop.getProperty(CIRCULAR_STRING) != null) {
			if (prop.getProperty(CIRCULAR_STRING).equals("yes")) {
				circular = true;
			}
		}

		subfunctions = new double[m][twoToK];
		masks = new int[m][k];

		// Initialize masks and subfunctions

		if (circular) {
			initializeMasksCircular();
		} else {
			initializaMasksNonCircular();
		}

		// Initialize subfunctions
		initializeSubfunctions();
	}

	private void initializeSubfunctions() {
		int twoToK = 1 << k;
		for (int sf = 0; sf < m; sf++) {
			for (int i = 0; i < twoToK; i++) {
				subfunctions[sf][i] = ((q > 0) ? rnd.nextInt(q) : rnd
						.nextDouble());
			}
		}
	}

	private void initializeMasksCircular() {
		for (int sf = 0; sf < m; sf++) {
			// Initialize masks
			masks[sf][0] = sf;
			for (int i = 1; i < k; i++) {
				masks[sf][i] = (sf + i) % n;
			}

		}
	}

	private void initializaMasksNonCircular() {
		int[] aux = new int[n - 1];
		for (int i = 0; i < n - 1; i++) {
			aux[i] = i;
		}

		for (int sf = 0; sf < m; sf++) {
			// Initialize masks

			masks[sf][0] = sf;
			// Shuffle the aux array to get k-1 random values from the n-1
			// values.
			for (int i = 0; i < k - 1; i++) {
				int r = i + rnd.nextInt(n - 1 - i);
				int v = aux[i];
				aux[i] = aux[r];
				aux[r] = v;
			}

			// Copy the other variables into the mask
			for (int i = 1; i < k; i++) {
				int v = aux[i - 1];
				if (v == sf) {
					masks[sf][i] = n - 1;
				} else {
					masks[sf][i] = v;
				}
			}

		}
	}

	public int getQ() {
		return q;
	}

	@Override
	public double evaluate(Solution sol) {

		PBSolution pbs = (PBSolution) sol;

		if (subfunctions == null) {
			throw new IllegalStateException(
					"The NK-landscape has not been configured");
		}

		double res = 0;

		for (int sf = 0; sf < m; sf++) {
			int index = 0;
			for (int i = k - 1; i >= 0; i--) {
				index = (index << 1) + pbs.getBit(masks[sf][i]);
			}
			res += subfunctions[sf][index];
		}

		return res;
	}

	public BigDecimal evaluateArbitraryPrecision(Solution sol) {
		PBSolution pbs = (PBSolution) sol;

		if (subfunctions == null) {
			throw new IllegalStateException(
					"The NK-landscape has not been configured");
		}

		BigDecimal res = BigDecimal.ZERO;

		for (int sf = 0; sf < m; sf++) {
			int index = 0;
			for (int i = k - 1; i >= 0; i--) {
				index = (index << 1) + pbs.getBit(masks[sf][i]);
			}
			res = res.add(new BigDecimal(subfunctions[sf][index]));
		}

		return res;
	}

	@Override
	public double evaluateSubfunction(int sf, PBSolution pbs) {
		if (subfunctions == null) {
			throw new IllegalStateException(
					"The NK-landscape has not been configured");
		}

		int index = 0;
		for (int i = k - 1; i >= 0; i--) {
			index = (index << 1) + pbs.getBit(i);
		}

		return subfunctions[sf][index];
	}

	public static void showHelp() {
		System.err.println("Arguments: <N> <K> <q> <c> [<seed>] [<solution>]");
		System.err.println("Use <q>=" + FORCE_NK
				+ " for NK landscapes (otherwise NKq-landscape is generated)");
		System.err.println("Use <q>=- for q=2^(K+1)");
		System.err
				.println("<c> hould be yes (for adjacent-model) or no (for random-model)");
		System.err
				.println("The instance is written in the standard output (if no solution is given)");
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			showHelp();
			return;
		}

		String n = args[0];
		String k = args[1];
		String q = args[2];
		String circular = args[3];
		PBSolution sol = null;
		long seed = 0;
		if (args.length >= 5) {
			seed = Long.parseLong(args[4]);
		} else {
			seed = Seeds.getSeed();
		}

		if (args.length >= 6) {
			sol = PBSolution.toPBSolution(args[5]);
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

		if (sol == null) {
			pbf.writeTo(new OutputStreamWriter(System.out));
		} else {
			BigDecimal res = pbf.evaluateArbitraryPrecision(sol);
			// System.out.println("Arbitrary precision optimal value: "+res);
			// System.out.println("Double: "+res.doubleValue());
		}

	}

	public boolean isCircular() {
		return circular;
	}

	public double[][] getSubFunctions() {
		return subfunctions;
	}

	public int getK() {
		return k;
	}

	public void writeTo(Writer wr) {
		PrintWriter pw = new PrintWriter(wr);

		pw.println("c NK-landscape instance");
		pw.println("c Generated by " + getClass().getName());
		pw.println("c The next line contains N and K");
		pw.println("p NK " + n + " " + (k - 1));
		pw.println("c For each subfunction first it appears the mask in a line starting with 'm'");
		pw.println("c The mask is a list of K+1 values with the indices of the variables in Big Endian order");
		pw.println("c Following the mask the subfunction is defined in a line without prefix");
		pw.println("c Random seed: " + seed);

		for (int i = 0; i < n; i++) {
			// Write the mask
			pw.print("m ");
			for (int j = k - 1; j >= 0; j--) {
				pw.print(masks[i][j] + " ");
			}
			pw.println();

			// Write the subfunction values
			for (int j = 0; j < subfunctions[i].length; j++) {
				pw.print(subfunctions[i][j] + " ");
			}
			pw.println();
		}

		pw.close();
	}

	public static void oldMain(String[] args) {

		if (args.length < 2) {
			System.out.println("Arguments: <n> <k> [<seed>]");
			return;
		}

		String n = args[0];
		String k = args[1];
		long seed = 0;

		if (args.length > 2) {
			seed = Long.parseLong(args[2]);
		}

		NKLandscapes nkl = new NKLandscapes();
		Properties prop = new Properties();
		prop.setProperty(NKLandscapes.N_STRING, n);
		prop.setProperty(NKLandscapes.K_STRING, k);
		nkl.setSeed(seed);
		nkl.setConfiguration(prop);

		int[][] ai = nkl.getAppearsIn();

		int min = ai[0].length;
		int max = ai[0].length;
		double sum = ai[0].length;

		int[] histogram = new int[ai.length];

		histogram[ai[0].length]++;

		for (int i = 1; i < ai.length; i++) {

			histogram[ai[i].length]++;
			sum += ai[i].length;

			if (ai[i].length > max) {
				max = ai[i].length;
			}

			if (ai[i].length < min) {
				min = ai[i].length;
			}
		}

		System.out.println("Min:" + min);
		System.out.println("Max:" + max);
		System.out.println("Avg:" + sum / ai.length);

		int p;
		int q;

		for (p = 0; p < ai.length && histogram[p] == 0; p++)
			;
		for (q = ai.length - 1; q >= 0 && histogram[q] == 0; q--)
			;

		System.out.println("Histogram [" + p + ".." + q + "]:"
				+ Arrays.toString(Arrays.copyOfRange(histogram, p, q + 1)));

	}

}
