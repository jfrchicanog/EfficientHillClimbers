package neo.landscape.theory.apps.pseudoboolean.experiments.loma;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.apache.commons.math3.fraction.BigFraction;

import neo.landscape.theory.apps.util.Process;

public class LocalOptimaMarkovModelCurves implements Process {

	public static enum PerturbationType {
		BIT_FLIP("bit-flip"),
		FIXED_HAMMING_DISTANCE("fixed-hamming"),
		FLIPS_WITH_REPLACEMENT("fixed-flips");

		private String name;
		PerturbationType(String name) {
			this.name =name;
		}

		public String getName() {
			return name;
		}

		public static Optional<PerturbationType> byName(String name) {
			return Arrays.stream(values())
					.filter(pert->pert.name.equals(name))
					.findFirst();
		}
	}
	
	public static interface ProbabilityDistribution {
		double getProbability(int d);
	}
	
	public static interface ProbabilityFamily {
		ProbabilityDistribution getProbabilityDistribution(int n, int alpha);
	}
	
	public static class BinomialProbabilityFamily implements ProbabilityFamily {
		private double bitflipProbability;

		@Override
		public ProbabilityDistribution getProbabilityDistribution(int n, int alpha) {
			bitflipProbability = ((double)alpha)/n;
			return d->Math.pow(bitflipProbability,d)*Math.pow(1-bitflipProbability,n-d);
		}
	}
	
	public static class HammingProbabilityFamily implements ProbabilityFamily {
		private static BigInteger binom(int n, int d) {
			BigInteger result = BigInteger.ONE;
			for (int dd=1; dd <= d; dd++) {
				result = result.multiply(BigInteger.valueOf(n));
				result = result.divide(BigInteger.valueOf(dd));
				n--;
			}
			return result;
		}
		
		@Override
		public ProbabilityDistribution getProbabilityDistribution(int n, int alpha) {
			double value = binom(n,alpha).doubleValue(); 
			return d->(d!=alpha)?0.0:1.0/value;
		}
	}
	
	// FIXME: this is wrong
	public static class PerturbationWithReplacementFamily implements ProbabilityFamily {
		private List<BigInteger> upsilon = new ArrayList<>();
		private BigInteger nToAlpha;
		private int n;
		private int alpha;
		
		@Override
		public ProbabilityDistribution getProbabilityDistribution(int n, int alpha) {
			this.n=n;
			this.alpha=alpha;
			nToAlpha = BigInteger.valueOf(n).pow(alpha);
			return this::computeProbability;
		}
		
		private void ensureUpsilon(int d) {
			if (upsilon.size() < d+1) {
				updateUpsilon(d);
			}
		}

		private void updateUpsilon(int d) {
			int previousD = upsilon.size()-1;
			if (previousD < 0) {
				upsilon.add(BigInteger.ZERO);
				previousD=0;
			}
			int k = previousD+1;
			while (k <= d) {
				BigInteger value = BigInteger.valueOf(k).pow(alpha);
				for (int l=0; l < k; l++) {
					BigInteger minuend = HammingProbabilityFamily.binom(k, l).multiply(upsilon.get(l));
					value = value.subtract(minuend);
				}
				upsilon.add(k, value);
				k++;
			}
		}
		
		private double computeProbability(int d) {
			ensureUpsilon(d);
			//BigInteger numerator = HammingProbabilityFamily.binom(n, d).multiply(upsilon.get(d));
			BigFraction fraction = new BigFraction(upsilon.get(d), nToAlpha);
			return fraction.doubleValue();
		}
		
	}

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
	
	// This is the state of the Markov model
	private double [][] state;
	private double order1Moment;
	private double order2Moment;
	
	// This is the output
	private PrintWriter output;

	public LocalOptimaMarkovModelCurves() {
		markovStrategy = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the evolution curves for the algorithm "
				+ "based on the dynamics computed by lo-markov-algorithm including the perturbation parameter";
	}

	@Override
	public String getID() {
		return "lo-markov-curves";
	}

	private List<String> getPerturbationTypes() {
		return Arrays.stream(PerturbationType.values())
				.map(PerturbationType::getName)
				.collect(Collectors.toList());
	}

	@Override
	public String getInvocationInfo() {
		return String.format("Arguments: %s <markov-algorithm-file> <perturbation type: %s> <alpha> <iterations>", getID(), getPerturbationTypes());
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 4) {
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
		
		int iterations = Integer.parseInt(args[3]);
		if (iterations < 0) {
			System.err.println(String.format("The number of iterations cannot be lower than zero; %d", iterations));
			return;
		}

		readMarkovStrategy(markovStrategyFile);
		if (alpha > n) {
			System.err.println(String.format("Alpha cannot be larger than n (=%d)",n));
			return;
		}
		
		computeTransitionMatrix(alpha);

		output = new PrintWriter(System.out, true);
		
		initializeState();
		computeStatistics();
		writeHeading();
		writeStatistics(0);
		for (int it=0; it < iterations; it++) {
			advanceState();
			computeStatistics();
			writeStatistics(it+1);
		}
		
		output.close();
	}
	
	private void advanceState() {
		int numberOfLocalOptima = fitness.length;
		int numberOfFitnessLevels = fitnessValue.length;
		double [][] newState = new double[numberOfFitnessLevels][numberOfLocalOptima];
		for (int level=0; level < numberOfFitnessLevels; level++) {
			for (int x=0; x < numberOfLocalOptima && fitness[x] <= level; x++) {
				for (int y=0; y < numberOfLocalOptima; y++) {
					int newLevel;
					if (fitness[y] > level) {
						newLevel = fitness[y];
					} else {
						newLevel = level;
					}
					newState[newLevel][y] += transitionMatrix[x][y] * state[level][x];
				}
			}
		}
		state = newState;
	}

	private void writeStatistics(int i) {
		try(Formatter formatter = new Formatter(Locale.US)) {
			output.println(formatter.format("%d\t%f\t%f", i, getMean(), getStdDev()).out().toString());
		}
	}

	private void writeHeading() {
		output.println("Iteration\tMean BestSoFar\tStdDev BestSoFar");
	}

	private void computeStatistics() {
		order1Moment = 0.0;
		order2Moment = 0.0;
		for (int i=0; i < fitnessValue.length; i++) {
			double value = fitnessValue[i];
			double psum = 0.0;
			for (int x=0; x < fitness.length && fitness[x] <= i; x++) {
				psum += state[i][x];
			}
			double product = psum * value;
			order1Moment += product;
			order2Moment += product * value;
		}
	}
	
	private double getStdDev() {
		double variance = order2Moment - order1Moment * order1Moment;
		if (variance <= 0) {
			return 0;
		} else {
			return Math.sqrt(variance);
		}
	}
	
	private double getMean() {
		return order1Moment;
	}

	private void initializeState() {
		int numberOfLocalOptima = fitness.length;
		state = new double [fitnessValue.length][numberOfLocalOptima];
		for (int x=0; x < numberOfLocalOptima; x++) {
			state[fitness[x]][x] = ((double)basin[x])/(1<<n);
		}
	}

	private void configureProbabiliytFamily() {
		switch (perturbation) {
		case FIXED_HAMMING_DISTANCE:
			family = new HammingProbabilityFamily();
			return;
		case BIT_FLIP:
			family = new BinomialProbabilityFamily();
			return;
		case FLIPS_WITH_REPLACEMENT:
			family = new PerturbationWithReplacementFamily();
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
