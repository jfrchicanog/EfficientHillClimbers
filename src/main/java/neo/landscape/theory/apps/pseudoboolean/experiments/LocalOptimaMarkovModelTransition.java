package neo.landscape.theory.apps.pseudoboolean.experiments;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import neo.landscape.theory.apps.pseudoboolean.experiments.LocalOptimaMarkovModelCurves.HammingProbabilityFamily;
import neo.landscape.theory.apps.pseudoboolean.experiments.LocalOptimaMarkovModelCurves.PerturbationType;
import neo.landscape.theory.apps.pseudoboolean.experiments.LocalOptimaMarkovModelCurves.ProbabilityDistribution;
import neo.landscape.theory.apps.pseudoboolean.experiments.LocalOptimaMarkovModelCurves.ProbabilityFamily;
import neo.landscape.theory.apps.util.Process;

public class LocalOptimaMarkovModelTransition implements Process {

	private PerturbationType perturbation;
	private ProbabilityFamily family;

	// This is what we read
	private Map<Triple<Integer, Integer, Integer>, Integer> markovStrategy;
	private int n;
	private int [] fitness;
	private double [] fitnessValue;
	private int [] basin;
	
	// This is the transition matrix
	private double [][] transitionMatrix;
	
	public LocalOptimaMarkovModelTransition() {
		markovStrategy = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the evolution curves for the algorithm "
				+ "based on the dynamics computed by lo-markov-algorithm including the perturbation parameter";
	}

	@Override
	public String getID() {
		return "lo-markov-transition";
	}

	private List<String> getPerturbationTypes() {
		return Arrays.stream(PerturbationType.values())
				.map(PerturbationType::getName)
				.collect(Collectors.toList());
	}

	@Override
	public String getInvocationInfo() {
		return String.format("Arguments: %s <markov-algorithm-file> <perturbation type: %s> <alpha>", getID(), getPerturbationTypes());
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 3) {
			System.err.println(getInvocationInfo());
			return;
		}

		String markovStrategyFile = args[0];
		String perturbationType = args[1];
		Optional<PerturbationType> optional = PerturbationType.byName(perturbationType);
		
		if (optional.isPresent()) {
			perturbation = optional.get();
			configureProbabiliytFamily();
		} else {
			System.err.println(String.format("Unrecognized perturbation type: %s",perturbationType));
			System.err.println(String.format("Valid options are %s",getPerturbationTypes()));
			return;
		}
		
		int alpha = Integer.parseInt(args[2]);
		if (alpha < 0) {
			System.err.println("Alpha cannot be less than zero");
			return;
		}

		readMarkovStrategy(markovStrategyFile);
		if (alpha > n) {
			System.err.println(String.format("Alpha cannot be larger than n (=%d)",n));
			return;
		}
		
		computeTransitionMatrix(alpha);

		try(PrintWriter output = new PrintWriter(System.out, true);
				Formatter formatter  = new Formatter(output, Locale.US)){
			
			writeLocalOptimaInformation(formatter);
			writeTransitionMatrix(formatter);
		}
	}
	
	private void writeTransitionMatrix(Formatter formatter) {
		formatter.format("LO from\tLO to\tProbability\n");
		int numberOfLocalOptima = fitness.length;
		for (int x=0; x < numberOfLocalOptima; x++) {
			for (int y=0; y < numberOfLocalOptima; y++) {
				formatter.format("%d\t%d\t%f\n",x,y,transitionMatrix[x][y]);
			}
		}
	}

	private void writeLocalOptimaInformation(Formatter formatter) {
		formatter.format("LO\tFitness\tSize of basin of attraction\n");
		int numberOfLocalOptima = fitness.length;
		for (int x=0; x < numberOfLocalOptima; x++) {
			formatter.format("%d\t%f\t%d\n",x,fitnessValue[fitness[x]],basin[x]);
		}
	}

	private void configureProbabiliytFamily() {
		switch (perturbation) {
		case FIXED_HAMMING_DISTANCE:
			family = new HammingProbabilityFamily();
			return;
		default:
			throw new IllegalArgumentException("Unsupported perturbation: "+perturbation);
		}
	}

	private void computeTransitionMatrix(int alpha) {
		ProbabilityDistribution distribution = family.getProbabilityDistribution(this.n, alpha);
		int numberOfLocalOptima = fitness.length;
		transitionMatrix = new double[numberOfLocalOptima][numberOfLocalOptima];
		
		for (int d=0; d <= n; d++) {
			double probability = distribution.getProbability(d);
			if (probability == 0.0) {
				continue;
			}
			
			for (int x=0; x < numberOfLocalOptima; x++) {
				for (int y=0; y < numberOfLocalOptima; y++) {
					transitionMatrix[x][y] += probability * markovStrategy.getOrDefault(Triple.of(x, d, y), 0);
				}
			}
		}		
	}

	private void readMarkovStrategy(String markovStrategyFile) {
		try(Stream<String> lineStream = Files.lines(Paths.get(markovStrategyFile))) {
			Map<Integer, Double> fitnessMap= new HashMap<>();
			Map<Integer, Integer> basin = new HashMap<>();
			lineStream.forEach(line->{
				if (!line.trim().isEmpty()) {
					String [] fields = line.split("\t");
					int n = fields.length - 5;
					if (n <= 0) {
						throw new IllegalStateException("There is a line with less elements than expected: "+line);
					} else if (this.n <= 0) {
						this.n = n;
					} else if (this.n != n) {
						throw new IllegalStateException("There is a line with different length: "+line);
					}

					if (fields[0].equals("LOX")) {
						// Header: continue
						return;
					}

					int x = Integer.parseInt(fields[0]);
					fitnessMap.computeIfAbsent(x, k->Double.parseDouble(fields[1]));
					basin.computeIfAbsent(x, k->Integer.parseInt(fields[2]));
					int y = Integer.parseInt(fields[3]);

					for (int d=0; d <= n; d++) {
						int sample = Integer.parseInt(fields[4+d]);
						this.markovStrategy.put(Triple.of(x, d, y), sample);
					}
				}
			});
			this.basin = IntStream.range(0, basin.size())
				.map(i->basin.get(i))
				.toArray();
			
			fitnessValue = fitnessMap.values().stream()
					.sorted()
					.distinct()
					.mapToDouble(Double::doubleValue)
					.toArray();
			Map<Double, Integer> inverseMap =IntStream.range(0, fitnessValue.length)
					.mapToObj(i->Pair.of(i, fitnessValue[i]))
					.collect(Collectors.toMap(Pair::getRight, Pair::getLeft));

			this.fitness = new int [fitnessMap.size()];
			fitnessMap.entrySet().forEach(entry->{
				this.fitness[entry.getKey()] = inverseMap.get(entry.getValue());
			});
			checkFitnessNonDecreasing();

		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private void checkFitnessNonDecreasing() {
		for (int i=0; i < fitness.length-1; i++) {
			if (fitness[i] > fitness[i+1]) {
				throw new IllegalStateException("Local optima should be in non-decreasing order in the input data");
			}
		}
	}

}
