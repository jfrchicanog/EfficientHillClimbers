package neo.landscape.theory.apps.pseudoboolean.experiments.loma;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class LocalOptimaMarkovModelComponents implements Process {

	// This is what we read
	private int nbLocalOptima = -1;
	
	// This is the transition matrix
	private double [][] transitionMatrix;
	private Map<Pair<Integer, Integer>, Double> transitionMap;
	
	// These are elements of the Tarjan algorithm
	private int number [];
	private int lowlink [];
	private TwoStatesIntegerSet numbered;
	private int component [];
	private Stack<Integer> stack;
	private int index;
	private int nbOfComponents;
	
	
	public LocalOptimaMarkovModelComponents() {
		transitionMap = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the communicating classes in the Markov chain "
				+ "produced by lo-markov-transition";
	}

	@Override
	public String getID() {
		return "lo-markov-components";
	}

	@Override
	public String getInvocationInfo() {
		return String.format("Arguments: %s <markov-transition-file>", getID());
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			System.err.println(getInvocationInfo());
			return;
		}

		String markovTransition = args[0];
		readMarkovStrategy(markovTransition);
		computeCommunicatingComponents();
		
		try(PrintWriter output = new PrintWriter(System.out, true);
				Formatter formatter  = new Formatter(output, Locale.US)){
			writeLocalOptimaInformation(formatter);
		}
	}
	
	private void computeCommunicatingComponents() {
		stack = new Stack<>();
		number = new int [nbLocalOptima];
		lowlink = new int [nbLocalOptima];
		numbered = new TwoStatesISArrayImpl(nbLocalOptima);
		component = new int [nbLocalOptima];
		nbOfComponents = 0;
		
		index=0;
		while (numbered.hasMoreUnexplored()) {
			int w = numbered.getNextUnexplored();
			strongConnect(w);
		}
	}
	
	private void strongConnect(int v) {
		index++;
		number[v] = index;
		numbered.explored(v);
		lowlink[v] = index;
		stack.add(v);
		for (int w=0; w < nbLocalOptima; w++) {
			if (transitionMatrix[v][w] > 0) {
				if (!numbered.isExplored(w)) {
					strongConnect(w);
					lowlink[v] = Math.min(lowlink[v], lowlink[w]);
				} else if (number[w] < number[v]) {
					if (stack.contains(w)) {
						lowlink[v] = Math.min(lowlink[v], number[w]);
					}
				}
			}
		}
		
		if (lowlink[v] == number[v]) {
			// start new strongly connected component
			nbOfComponents++;
			while (!stack.isEmpty() && number[stack.peek()] >= number[v]) {
				int w = stack.pop();
				component[w] = nbOfComponents;
			}
		}
	}

	private void switchToArray() {
		if (nbLocalOptima < 0) {
			throw new IllegalStateException("Number of local optima not initialized");
		}
		transitionMatrix = new double [nbLocalOptima][nbLocalOptima];
	}
	
	private void writeLocalOptimaInformation(Formatter formatter) {
		formatter.format("LO\tCommunicating component\n");
		for (int lo=0; lo < nbLocalOptima; lo++) {
			formatter.format("%d\t%d\n",lo,component[lo]);
		}
	}
	
	private void readMarkovStrategy(String markovStrategyFile) {
		try(Stream<String> lineStream = Files.lines(Paths.get(markovStrategyFile))) {
			ThreadLocal<Boolean> go = new ThreadLocal<>();
			go.set(false);
			lineStream.forEach(line->{
				if (!line.trim().isEmpty()) {
					String [] fields = line.split("\t");
					if (!go.get()) {
						if (fields[0].equals("LO from")) {
							go.set(true);
						}
						return;
					}
					
					int x = Integer.parseInt(fields[0]);
					int y = Integer.parseInt(fields[1]);
					double prob = Double.parseDouble(fields[2]);
					
					checkIfSwithcIsPossible(y);
					putTransitionProbability(x,y,prob);
				}
			});
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void putTransitionProbability(int x, int y, double prob) {
		if (transitionMatrix != null) {
			transitionMatrix[x][y] = prob;
		} else {
			transitionMap.put(Pair.of(x, y), prob);
		}
	}
	
	private void checkIfSwithcIsPossible(int y) {
		if (transitionMap == null) {
			// Switch already done
			return;
		}
		if (y > nbLocalOptima) {
			nbLocalOptima = y;
		} else {
			nbLocalOptima = nbLocalOptima+1;
			switchToArray();
			transitionMap.entrySet().stream().forEach(e->{
				Pair<Integer, Integer> pair = e.getKey();
				putTransitionProbability(pair.getLeft(),pair.getRight(), e.getValue());
			});
			transitionMap = null;
		}
	}
}
