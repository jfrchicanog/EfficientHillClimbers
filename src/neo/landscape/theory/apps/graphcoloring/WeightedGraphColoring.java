package neo.landscape.theory.apps.graphcoloring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.Problem;
import neo.landscape.theory.apps.efficienthc.Solution;

public class WeightedGraphColoring implements Problem {

	// The instance should be in DIMACS format
	public static final String INSTANCE_NAME = "instance";
	public static final String COLORS = "colors";

	public static final String[] colorNames = { "blue", "red", "green",
			"purple", "yellow", "white", "black" };

	private long seed;
	private int vertices;
	private int edges;
	private int max_degree;

	public int getEdges() {
		return edges;
	}

	private int[][] adjacents;

	private Random rnd;
	private double[][] weights;
	private int colors;

	private boolean equalWeights;

	public WeightedGraphColoring() {
		rnd = new Random(seed);
	}

	public int getVertices() {
		return vertices;
	}

	public int[][] getAdjacents() {
		return adjacents;
	}

	public double[][] getWeights() {
		return weights;
	}

	public int getColors() {
		return colors;
	}

	public boolean isEqualWeights() {
		return equalWeights;
	}

	@Override
	public void setSeed(long seed) {
		this.seed = seed;
		rnd = new Random(seed);
	}

	@Override
	public void setConfiguration(Properties prop) {
		File f = new File(prop.getProperty(INSTANCE_NAME));
		Reader rd;
		try {
			rd = new InputStreamReader(new FileInputStream(f));
			readInstance(rd);
			rd.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String color_str = prop.getProperty(COLORS);
		if (color_str != null) {
			colors = Integer.parseInt(color_str);
		}
	}

	public int getMaxDegree() {
		return max_degree;
	}

	private void readInstance(Reader rd) throws IOException {
		Set<Integer>[] aux_adjs = null;

		BufferedReader brd = new BufferedReader(rd);

		String line;

		while ((line = brd.readLine()) != null) {
			switch (line.charAt(0)) {
			case 'c':
				break;
			case 'p':
				String[] str = line.split("[ \t]");
				vertices = Integer.parseInt(str[2]);
				edges = Integer.parseInt(str[3]);
				aux_adjs = new Set[vertices];
				break;
			case 'e':
				str = line.split("[ \t]");
				int u = Integer.parseInt(str[1]) - 1;
				int v = Integer.parseInt(str[2]) - 1;
				addEdge(aux_adjs, u, v);
				addEdge(aux_adjs, v, u);

				break;
			}
		}

		adjacents = new int[aux_adjs.length][];
		int sum_degree = 0;
		max_degree = 0;
		for (int i = 0; i < adjacents.length; i++) {
			if (aux_adjs[i] == null) {
				adjacents[i] = new int[0];
			} else {
				adjacents[i] = new int[aux_adjs[i].size()];
				int j = 0;

				for (int val : aux_adjs[i]) {
					adjacents[i][j++] = val;
				}

				if (j > max_degree) {
					max_degree = j;
				}

				sum_degree += j;

			}
		}

		edges = sum_degree / 2;

		equalWeights = true;

	}

	private void addEdge(Set<Integer>[] aux_adjs, int u, int v) {
		if (aux_adjs[u] == null) {
			aux_adjs[u] = new HashSet<Integer>();
		}

		if (aux_adjs[v] == null) {
			aux_adjs[v] = new HashSet<Integer>();
		}

		aux_adjs[u].add(v);
		aux_adjs[v].add(u);

	}

	@Override
	public WGCSolution getRandomSolution() {
		WGCSolution sol = new WGCSolution();
		sol.colors = new int[vertices];
		for (int i = 0; i < sol.colors.length; i++) {
			sol.colors[i] = rnd.nextInt(colors);
		}
		return sol;
	}

	@Override
	public BigDecimal evaluateArbitraryPrecision(Solution sol) {
		return new BigDecimal(evaluate(sol));
	}

	public double evaluate(WGCSolution sol) {

		double cost = 0;

		for (int u = 0; u < vertices; u++) {
			for (int v : adjacents[u]) {
				if (v > u && sol.colors[u] == sol.colors[v]) {
					cost += equalWeights ? 1 : weights[u][v];
				}
			}
		}

		return cost;
	}

	@Override
	public <P extends Problem> double evaluate(Solution<P> sol) {
		if (sol instanceof WGCSolution) {
			return evaluate((WGCSolution) sol);
		} else
			throw new IllegalArgumentException(
					"Expected a solution of class WGCSolution but obtained one of class "
							+ sol.getClass().getCanonicalName());
	}

	public String toString() {
		String str = "Weighted Graph Coloring\n";

		str += "nodes=" + vertices + ", edges=" + edges + "\n";
		for (int u = 0; u < vertices; u++) {
			str += u + " " + Arrays.toString(adjacents[u]) + "\n";
		}

		return str;
	}

	public String dotLanguage(WGCSolution sol) {
		return dotLanguage(sol, null);
	}

	private String vertexName(int v, WGCMove m) {
		if (m == null || m.vertex != v) {
			return "\"" + v + "\"";
		} else {
			return "\"" + m.vertex + "(" + m.improvement + ")\"";
		}
	}

	public String dotLanguage(WGCSolution sol, WGCMove m) {
		String str = "graph g {\n";

		for (int i = 0; i < sol.colors.length; i++) {
			if (m != null && i == m.vertex) {
				str += vertexName(i, m)
						+ " [shape=doublecircle style=filled color="
						+ colorString(sol.colors[i]) + " fontcolor="
						+ colorString(m.color) + "];\n";
			} else {
				str += "\"" + i + "\" [style=filled color="
						+ colorString(sol.colors[i]) + "];\n";
			}
		}

		for (int u = 0; u < vertices; u++) {
			for (int v : adjacents[u]) {
				if (v > u) {
					str += vertexName(u, m) + "--" + vertexName(v, m) + " ";
					if (sol.colors[u] == sol.colors[v]) {
						str += "[penwidth=5]";
					}
					str += "\n";

				}
			}
		}

		str += "}\n";

		return str;
	}

	private String colorString(int i) {
		return colorNames[i % colorNames.length];
	}

	public static void main(String args[]) throws Exception {

		if (args.length < 2) {
			System.out.println("Arguments: <instance> <colors> [<trials>]");
			return;
		}

		Properties prop = new Properties();
		prop.setProperty("instance", args[0]);
		prop.setProperty("colors", args[1]);

		int trials = 1;

		WeightedGraphColoring wgc = new WeightedGraphColoring();
		wgc.setConfiguration(prop);

		if (args.length > 2) {
			trials = Integer.parseInt(args[2]);
		}

		System.out.println(wgc);

		for (int i = 0; i < trials; i++) {
			WGCSolution sol = wgc.getRandomSolution();
			System.out.println(sol);
			System.out.println("Fitness:" + wgc.evaluate(sol));

			File dot = new File("g.dot");
			FileOutputStream fos = new FileOutputStream(dot);
			PrintWriter pw = new PrintWriter(fos);
			pw.println(wgc.dotLanguage(sol));
			pw.close();

		}
	}

	public double getWeight(int u, int v) {
		return equalWeights ? 1 : weights[u][v];
	}

}
