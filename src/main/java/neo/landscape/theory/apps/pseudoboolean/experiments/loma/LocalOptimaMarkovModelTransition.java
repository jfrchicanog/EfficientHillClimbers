package neo.landscape.theory.apps.pseudoboolean.experiments.loma;

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
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

import neo.landscape.theory.apps.pseudoboolean.experiments.loma.LocalOptimaMarkovModelCurves.HammingProbabilityFamily;
import neo.landscape.theory.apps.pseudoboolean.experiments.loma.LocalOptimaMarkovModelCurves.PerturbationType;
import neo.landscape.theory.apps.pseudoboolean.experiments.loma.LocalOptimaMarkovModelCurves.ProbabilityDistribution;
import neo.landscape.theory.apps.pseudoboolean.experiments.loma.LocalOptimaMarkovModelCurves.ProbabilityFamily;
import neo.landscape.theory.apps.util.Process;

public class LocalOptimaMarkovModelTransition implements Process {

	private PerturbationType perturbation;
	private ProbabilityFamily family;

	// This is what we read
	private Map<Triple<Integer, Integer, Integer>, Integer> markovStrategy;
	private int [] markovStrategyArray;
	private int n;
	private int [] fitness;
	private double [] fitnessValue;
	private int [] basin;
	
	// This is what we compute
	private double [] expectedHittingTime;
	private double [] stationaryDistribution;
	private int numberOfGlobalOptima;
	private double [] probabilityOfHittingGlobalOptima;
	private double [] expectedHittingTimeIfGlobalOptimaReached;
	private int nbLocalOptima = -1;

	// This is the transition matrix
	private double [][] transitionMatrix;

	public LocalOptimaMarkovModelTransition() {
		markovStrategy = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the transition probability matrix for the algorithm "
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
		computeExpectedHittingTime();
		computeStationaryProbability();
		computeGlobalOptimaExpectedHittingTime();

		try(PrintWriter output = new PrintWriter(System.out, true);
				Formatter formatter  = new Formatter(output, Locale.US)){

			writeLocalOptimaInformation(formatter);
			writeGlobalOptimaInformation(formatter);
			writeTransitionMatrix(formatter);
		}
	}

	private double delta(int i, int j) {
		return i==j?1:0;
	}

	private void putMarkovStrategyInfo(int x, int d, int w, final int sample) {
		if (markovStrategyArray != null) {
			markovStrategyArray[markovSampleCoordinates(x, d, w)] += sample;
		} else {
			markovStrategy.put(Triple.of(x, d, w), sample);
		}
	}

	private int markovSampleCoordinates (int x, int d, int y) {
		return (x*nbLocalOptima+y)*(n+1)+d;
	}

	private void switchToArray() {
		if (nbLocalOptima < 0 || n <= 0) {
			throw new IllegalStateException("Number of local optima not initialized");
		}
		markovStrategyArray = new int [nbLocalOptima * nbLocalOptima * (n+1)];
	}

	private void computeExpectedHittingTime() {
		int numberOfLocalOptima = basin.length;
		expectedHittingTime = new double [numberOfLocalOptima]; // TODO: this should be fraction

		RealMatrix matrix = MatrixUtils.createRealMatrix(numberOfLocalOptima-1, numberOfLocalOptima-1); // TODO: Matrix of fraction
		RealVector ones = new ArrayRealVector(numberOfLocalOptima-1, 1.0); // TODO: fraction
		for (int lo=0; lo < numberOfLocalOptima; lo++) {
			for (int i=0, ii=0; i < numberOfLocalOptima; i++) {
				if (i!=lo) {
					for (int j=0, jj=0; j < numberOfLocalOptima; j++) {
						if (j!=lo) {
							matrix.setEntry(ii, jj, delta(ii,jj)-transitionMatrix[i][j]);
							jj++;
						}
					}
					ii++;
				}
			}

			try {
				DecompositionSolver solver = new LUDecomposition(matrix).getSolver(); // TODO: fraction
				RealVector hittingTimes = solver.solve(ones); // TODO: Fractions

				expectedHittingTime[lo] = 0;

				for (int i=0, ii=0; i < numberOfLocalOptima; i++) {
					if (i!=lo) {
						expectedHittingTime[lo] += (basin[i] * hittingTimes.getEntry(ii))/(1 << n);
						ii++;
					}
				}
			} catch (SingularMatrixException e) {
				// FIXME: I am assuming here that the Markov chain is reducible
				expectedHittingTime[lo] = Double.POSITIVE_INFINITY;
			}
		}
	}

	private void computeGlobalOptimaExpectedHittingTime() {
		int numberOfLocalOptima = basin.length;
		int goStartIndex = numberOfLocalOptima-numberOfGlobalOptima;

		RealMatrix matrix = MatrixUtils.createRealMatrix(numberOfLocalOptima-numberOfGlobalOptima, numberOfLocalOptima-numberOfGlobalOptima);

		for (int i=0; i < goStartIndex; i++) {
			for (int j=0; j < goStartIndex; j++) {
				matrix.setEntry(i, j, delta(i,j)-transitionMatrix[i][j]);
			}
		}

		RealMatrix recurrent = MatrixUtils.createRealMatrix(numberOfLocalOptima-numberOfGlobalOptima, numberOfGlobalOptima);

		for (int i=0; i < goStartIndex; i++) {
			for (int j=0; j < numberOfGlobalOptima; j++) {
				recurrent.setEntry(i, j, transitionMatrix[i][j+goStartIndex]);
			}
		}

		try {
			RealMatrix inverse = MatrixUtils.inverse(matrix);
			RealMatrix probs = inverse.multiply(recurrent);
			RealMatrix times = inverse.multiply(probs);

			// Check probs
			//		RealVector ones = new ArrayRealVector(numberOfGlobalOptima, 1.0);
			//		RealVector shouldBeOnes = probs.operate(ones);
			//		System.err.println(shouldBeOnes);
			//		for (int i=0; i < shouldBeOnes.getDimension(); i++) {
			//			if (Math.abs(shouldBeOnes.getEntry(i)-1.0) > 0.01) {
			//				System.err.println("Arg");
			//			}
			//		}

			RealVector initial = new ArrayRealVector(numberOfLocalOptima-numberOfGlobalOptima);
			for (int i = 0; i < goStartIndex; i++) {
				initial.setEntry(i, ((double)basin[i])/(1<<n));
			}

			RealVector goals = new ArrayRealVector(numberOfGlobalOptima);
			for (int i=0; i < numberOfGlobalOptima; i++) {
				goals.setEntry(i, ((double)basin[i+goStartIndex])/(1<<n));
			}

			RealVector probsGo = probs.preMultiply(initial);
			probsGo = probsGo.add(goals);

			RealVector timesGo = times.preMultiply(initial);

			RealVector ones = new ArrayRealVector(numberOfGlobalOptima, 1.0);
			double val = ones.dotProduct(probsGo);
			//System.err.println("Prob: "+val);

			/*
		RealVector onesTrans = new ArrayRealVector(numberOfLocalOptima-numberOfGlobalOptima, 1.0);
		RealVector expected = inverse.operate(onesTrans);
		double totalExpectedTime = expected.dotProduct(initial);		
		System.err.println("Total expected time: "+totalExpectedTime);
		double shouldBeExpected = timesGo.dotProduct(ones);
		System.err.println("Should expected time: "+shouldBeExpected);
			 */

			probabilityOfHittingGlobalOptima = probsGo.toArray();
			timesGo = timesGo.ebeDivide(probsGo);
			expectedHittingTimeIfGlobalOptimaReached = timesGo.toArray();
		} catch (SingularMatrixException e) {
			probabilityOfHittingGlobalOptima = new double[numberOfGlobalOptima];
			Arrays.fill(probabilityOfHittingGlobalOptima, Double.NaN);
			
			expectedHittingTimeIfGlobalOptimaReached = new double [numberOfGlobalOptima];
			Arrays.fill(expectedHittingTimeIfGlobalOptimaReached, Double.NaN);
		}

	}


	private void computeStationaryProbability() {
		int numberOfLocalOptima = basin.length;
		RealMatrix matrix = MatrixUtils.createRealMatrix(numberOfLocalOptima, numberOfLocalOptima);
		RealVector constant = new ArrayRealVector(numberOfLocalOptima);
		constant.setEntry(numberOfLocalOptima-1, 1.0);
		for (int i=0; i < numberOfLocalOptima-1; i++) {
			for (int j=0; j < numberOfLocalOptima; j++) {
				matrix.setEntry(i, j, transitionMatrix[j][i]-delta(i,j));
			}
		}
		for (int j=0; j < numberOfLocalOptima; j++) {
			matrix.setEntry(numberOfLocalOptima-1, j, 1.0);
		}

		try {
			DecompositionSolver solver = new LUDecomposition(matrix).getSolver();
			RealVector result = solver.solve(constant);
			stationaryDistribution = result.toArray();
		} catch (SingularMatrixException e) {
			// FIXME: I am assuming here that the Markov chain is reducible
			stationaryDistribution = new double [numberOfLocalOptima];
			for (int j=0; j < numberOfLocalOptima; j++) {
				stationaryDistribution[j] = Double.NaN;
			}
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
		formatter.format("LO\tFitness\tSize of basin of attraction\tExpected hitting time\tStationary distribution\n");
		int numberOfLocalOptima = fitness.length;
		for (int lo=0; lo < numberOfLocalOptima; lo++) {
			formatter.format("%d\t%f\t%d\t%f\t%f\n",lo,fitnessValue[fitness[lo]],basin[lo],expectedHittingTime[lo], stationaryDistribution[lo]);
		}
	}

	private void writeGlobalOptimaInformation(Formatter formatter) {
		formatter.format("GO\tHitting probability\tExpected hitting time if reached\n");
		int goStartIndex = fitness.length-numberOfGlobalOptima;
		for (int go=0; go < numberOfGlobalOptima; go++) {
			formatter.format("%d\t%f\t%f\n",go+goStartIndex,probabilityOfHittingGlobalOptima[go], expectedHittingTimeIfGlobalOptimaReached[go]);
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
		transitionMatrix = new double[numberOfLocalOptima][numberOfLocalOptima]; // TODO: this should be fraction

		for (int d=0; d <= n; d++) {
			double probability = distribution.getProbability(d); // TODO: this should be fraction
			if (probability == 0.0) {
				continue;
			}

			for (int x=0; x < numberOfLocalOptima; x++) {
				for (int y=0; y < numberOfLocalOptima; y++) {
					transitionMatrix[x][y] += probability * markovStrategyArray[markovSampleCoordinates(x, d, y)];
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

					checkIfSwithcIsPossible(y);

					for (int d=0; d <= n; d++) {
						int sample = Integer.parseInt(fields[4+d]);
						putMarkovStrategyInfo(x, d, y, sample);
						//this.markovStrategy.put(Triple.of(x, d, y), sample);
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
			computeNumberOfGlobalOptima();
			checkFitnessNonDecreasing();

		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private void checkIfSwithcIsPossible(int y) {
		if (markovStrategy == null) {
			// Switch already done
			return;
		}
		if (y > nbLocalOptima) {
			nbLocalOptima = y;
		} else {
			nbLocalOptima = nbLocalOptima+1;
			switchToArray();
			markovStrategy.entrySet().stream().forEach(e->{
				Triple<Integer, Integer,  Integer> triple = e.getKey();
				putMarkovStrategyInfo(triple.getLeft(), triple.getMiddle(), triple.getRight(), e.getValue());
			});
			markovStrategy = null;
		}
	}

	private void computeNumberOfGlobalOptima() {
		numberOfGlobalOptima = 0;
		for (int lo=0; lo < fitness.length; lo++) {
			if (fitness[lo] == fitnessValue.length-1) {
				numberOfGlobalOptima++;
			}
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
