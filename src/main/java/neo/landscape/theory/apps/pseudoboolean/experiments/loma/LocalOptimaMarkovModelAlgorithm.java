package neo.landscape.theory.apps.pseudoboolean.experiments.loma;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import neo.landscape.theory.apps.util.Process;

public class LocalOptimaMarkovModelAlgorithm implements Process {

	public static enum Algorithm {
		ILS("ils"),
		DRILS("drils"),
		DRILS_ELITIST("drils-elitist"),
		ILS_NON_ELITIST("ils-non-elitist"),
		;

		private String name;
		Algorithm(String name) {
			this.name=name;
		}

		public String getName() {
			return name;
		}

		public static Optional<Algorithm> byName(String name) {
			return Arrays.stream(Algorithm.values())
					.filter(alg->alg.name.equals(name))
					.findFirst();
		}

	}

	// This is the algorithm we are using
	private Algorithm algorithm;

	// This is what we read
	private Map<Triple<Integer, Integer, Integer>, Integer> markovSample;
	private Map<Integer, Double> fitness;
	private int [] basin;
	private Map<Pair<Integer, Integer>, Integer> crossover;
	private int n;


	// This is what we produce
	private Map<Triple<Integer, Integer, Integer>, Integer> markovStrategy;
	private PrintWriter markovModel;

	public LocalOptimaMarkovModelAlgorithm() {
		markovSample = new HashMap<>();
		fitness = new HashMap<>();
		crossover = new HashMap<>();
		markovStrategy = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes adatps the instance-specific information computed "
				+ "by lo-markov-extraction to consider a search algorithm strategy (without perturbation parameter)";
	}

	@Override
	public String getID() {
		return "lo-markov-algorithm";
	}

	public List<String> getAlgorithmNames() {
		return Arrays.stream(Algorithm.values())
				.map(Algorithm::getName)
				.collect(Collectors.toList());
	}

	@Override
	public String getInvocationInfo() {
		return String.format("Arguments: %s <markov-extraction-file> <algorithm: %s>", getID(), getAlgorithmNames());
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 2) {
			System.err.println(getInvocationInfo());
			return;
		}

		String markovSampleFile = args[0];
		String algorithm = args[1];
		Optional<Algorithm> alg = Algorithm.byName(algorithm);
		if (alg.isPresent()) {
			this.algorithm = alg.get();
		} else {
			System.err.println(String.format("Algorithm %s not recognized", algorithm));
			System.err.println(String.format("Valid options are %s",getAlgorithmNames()));
			return;
		}

		readMarkovSample(markovSampleFile);

		int numberOfLocalOptima = fitness.size();
		basin = new int[numberOfLocalOptima];
		for (int x = 0; x < numberOfLocalOptima; x++) {
			for (int y=0; y < numberOfLocalOptima; y++) {
				int sumSamples = 0;
				for (int d = 0; d <= n; d++) {
					int w = acceptance(y, x);
					final int sample = markovSample.getOrDefault(Triple.of(x, d, y),0);
					sumSamples += sample;
					markovStrategy.compute(Triple.of(x, d, w), (k,v)->((v==null)?0:v)+sample);
				}
				basin[y] = sumSamples;
			}
		}

		markovModel = new PrintWriter(System.out, true);

		// Printing header
		markovModel.print("LOX\tFITNESS_X\tBASIN\tLOW\t");
		for (int d=0; d < n; d++) {
			markovModel.print("D"+d+"\t");
		}
		markovModel.println("D"+n);
		// Printing data
		for (int indX = 0; indX < numberOfLocalOptima; indX++) {
			double fitness = this.fitness.get(indX);
			for (int indW=0; indW < numberOfLocalOptima; indW++) {
				markovModel.print(indX+"\t"+fitness+"\t"+basin[indX]+"\t"+indW+"\t");
				for (int d=0; d < n; d++) {
					markovModel.print(markovStrategy.getOrDefault(Triple.of(indX, d, indW), 0)+"\t");
				}
				markovModel.println(markovStrategy.getOrDefault(Triple.of(indX, n, indW), 0));
			}
		}
		markovModel.close();

	}

	private int acceptance(int y, int x) {
		switch (algorithm) {
		case ILS:
			return elitistStrategy(y, x);
		case ILS_NON_ELITIST:
			return y;
		case DRILS:
			int z = crossover.get(Pair.of(y,x));
			if (z == x || z == y) {
				return y;
			} else {
				return z;
			}
		case DRILS_ELITIST:
			int zz = crossover.get(Pair.of(y,x));
			if (zz == x || zz == y) {
				return elitistStrategy(y, x);
			} else {
				return zz;
			}
		default:
			throw new IllegalStateException("There is no algorithm specificed or algorithm not considered: "+algorithm);
		}
	}

	private int elitistStrategy(int second, int first) {
		if (fitness.get(second) >= fitness.get(first)) {
			return second;
		} else {
			return first;
		}
	}

	private void readMarkovSample(String markovSampleFile) {
		try(Stream<String> lineStream = Files.lines(Paths.get(markovSampleFile))) {
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
					double fitness = Double.parseDouble(fields[1]);
					this.fitness.computeIfAbsent(x, k->fitness);
					int y = Integer.parseInt(fields[2]);
					int z = Integer.parseInt(fields[3]);
					this.crossover.put(Pair.of(x,y), z);
					
					for (int d=0; d <= n; d++) {
						int sample = Integer.parseInt(fields[4+d]);
						this.markovSample.put(Triple.of(x, d, y), sample);
					}
				}
			});
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
