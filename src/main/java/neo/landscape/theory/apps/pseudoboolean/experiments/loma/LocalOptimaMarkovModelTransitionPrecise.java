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
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.fraction.BigFractionField;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.ArrayFieldVector;
import org.apache.commons.math3.linear.FieldDecompositionSolver;
import org.apache.commons.math3.linear.FieldLUDecomposition;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.FieldVector;
import org.apache.commons.math3.linear.SingularMatrixException;

import neo.landscape.theory.apps.pseudoboolean.experiments.loma.LocalOptimaMarkovModelCurves.PerturbationType;
import neo.landscape.theory.apps.util.Process;
import neo.landscape.theory.apps.util.TwoStatesISArrayImpl;
import neo.landscape.theory.apps.util.TwoStatesIntegerSet;

public class LocalOptimaMarkovModelTransitionPrecise implements Process {

	
	public static interface ProbabilityDistribution {
		BigFraction getProbability(int d);
	}
	
	public static interface ProbabilityFamily {
		ProbabilityDistribution getProbabilityDistribution(int n, int alpha);
	}
	
	public static class BinomialProbabilityFamily implements ProbabilityFamily {
		private BigFraction bitflipProbability;

		@Override
		public ProbabilityDistribution getProbabilityDistribution(int n, int alpha) {
			bitflipProbability = new BigFraction(alpha,n);
			return d->bitflipProbability.pow(d).multiply(BigFraction.ONE.subtract(bitflipProbability).pow(n-d));
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
			BigFraction value = new BigFraction(binom(n,alpha)); 
			return d->(d!=alpha)?BigFraction.ZERO:value.pow(-1);
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
		
		private BigFraction computeProbability(int d) {
			ensureUpsilon(d);
			//BigInteger numerator = HammingProbabilityFamily.binom(n, d).multiply(upsilon.get(d));
			BigFraction fraction = new BigFraction(upsilon.get(d), nToAlpha);
			return fraction;
		}
	}
	
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
	private BigFraction [] expectedHittingTime;
	private BigFraction [] stationaryDistribution;
	private int numberOfGlobalOptima;
	private BigFraction [] probabilityOfHittingGlobalOptima;
	private BigFraction [] expectedHittingTimeIfGlobalOptimaReached;
	private int nbLocalOptima = -1;

	// This is the transition matrix
	private BigFraction [][] transitionMatrix;
	private boolean doubleOutput = false;

	// These are elements of the Tarjan algorithm
	private int number [];
	private int lowlink [];
	private TwoStatesIntegerSet numbered;
	private int component [];
	private Stack<Integer> stack;
	private int index;
	private int nbOfComponents;

	public LocalOptimaMarkovModelTransitionPrecise() {
		markovStrategy = new HashMap<>();
	}

	@Override
	public String getDescription() {
		return "This experiment computes the transition probability matrix for the algorithm "
				+ "based on the dynamics computed by lo-markov-algorithm including the perturbation parameter";
	}

	@Override
	public String getID() {
		return "lo-markov-transition-precise";
	}

	private List<String> getPerturbationTypes() {
		return Arrays.stream(PerturbationType.values())
				.map(PerturbationType::getName)
				.collect(Collectors.toList());
	}

	@Override
	public String getInvocationInfo() {
		return String.format("Arguments: %s <markov-algorithm-file> <perturbation type: %s> <alpha> [-double]", getID(), getPerturbationTypes());
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
		
		if (args.length > 3 && args[3].equals("-double")) {
			doubleOutput = true;
		}

		readMarkovStrategy(markovStrategyFile);
		if (alpha > n) {
			System.err.println(String.format("Alpha cannot be larger than n (=%d)",n));
			return;
		}

		computeTransitionMatrix(alpha);
		computeCommunicatingComponents();
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

	private BigFraction delta(int i, int j) {
		return i==j?BigFraction.ONE:BigFraction.ZERO;
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
		expectedHittingTime = new BigFraction [numberOfLocalOptima];

		Array2DRowFieldMatrix<BigFraction> matrix = new Array2DRowFieldMatrix<BigFraction>(BigFractionField.getInstance(), numberOfLocalOptima-1, numberOfLocalOptima-1);
		FieldVector<BigFraction> ones = new ArrayFieldVector<BigFraction>(numberOfLocalOptima-1, BigFraction.ONE);
		for (int lo=0; lo < numberOfLocalOptima; lo++) {
			for (int i=0, ii=0; i < numberOfLocalOptima; i++) {
				if (i!=lo) {
					for (int j=0, jj=0; j < numberOfLocalOptima; j++) {
						if (j!=lo) {
							matrix.setEntry(ii, jj, delta(ii,jj).subtract(transitionMatrix[i][j]));
							jj++;
						}
					}
					ii++;
				}
			}

			try {
				
				FieldDecompositionSolver<BigFraction> solver = new FieldLUDecomposition<BigFraction>(matrix).getSolver();
				FieldVector<BigFraction> hittingTimes = solver.solve(ones);

				expectedHittingTime[lo] = BigFraction.ZERO;

				for (int i=0, ii=0; i < numberOfLocalOptima; i++) {
					if (i!=lo) {
						BigFraction intermediate = (new BigFraction(basin[i],1<<n)).multiply(hittingTimes.getEntry(ii));
						expectedHittingTime[lo] = expectedHittingTime[lo].add(intermediate);
						ii++;
					}
				}
			} catch (SingularMatrixException e) {
				// FIXME: I am assuming here that the Markov chain is reducible
				expectedHittingTime[lo] = null;
			}
		}
	}

	private void computeGlobalOptimaExpectedHittingTime() {
		int numberOfLocalOptima = basin.length;
		int goStartIndex = numberOfLocalOptima-numberOfGlobalOptima;

		Array2DRowFieldMatrix<BigFraction> matrix = new Array2DRowFieldMatrix<BigFraction>(BigFractionField.getInstance(), 
				goStartIndex, 
				goStartIndex);
		for (int i=0; i < goStartIndex; i++) {
			for (int j=0; j < goStartIndex; j++) {
				matrix.setEntry(i, j, delta(i,j).subtract(transitionMatrix[i][j]));
			}
		}

		
		Array2DRowFieldMatrix<BigFraction> recurrent = new Array2DRowFieldMatrix<BigFraction>(BigFractionField.getInstance(), 
				goStartIndex, 
				numberOfGlobalOptima);
		
		for (int i=0; i < goStartIndex; i++) {
			for (int j=0; j < numberOfGlobalOptima; j++) {
				recurrent.setEntry(i, j, transitionMatrix[i][j+goStartIndex]);
			}
		}

		try {
			FieldMatrix<BigFraction> inverse = new FieldLUDecomposition<BigFraction>(matrix).getSolver().getInverse();
			FieldMatrix<BigFraction> probs = inverse.multiply(recurrent);
			FieldMatrix<BigFraction> times = inverse.multiply(probs);

			// Check probs
			//		RealVector ones = new ArrayRealVector(numberOfGlobalOptima, 1.0);
			//		RealVector shouldBeOnes = probs.operate(ones);
			//		System.err.println(shouldBeOnes);
			//		for (int i=0; i < shouldBeOnes.getDimension(); i++) {
			//			if (Math.abs(shouldBeOnes.getEntry(i)-1.0) > 0.01) {
			//				System.err.println("Arg");
			//			}
			//		}

			FieldVector<BigFraction> initial = new ArrayFieldVector<>(BigFractionField.getInstance(),goStartIndex);
			for (int i = 0; i < goStartIndex; i++) {
				initial.setEntry(i, new BigFraction(basin[i],1<<n));
			}

			FieldVector<BigFraction> goals = new ArrayFieldVector<>(BigFractionField.getInstance(),numberOfGlobalOptima);
			for (int i=0; i < numberOfGlobalOptima; i++) {
				
				goals.setEntry(i, new BigFraction(basin[i+goStartIndex],1<<n));
			}

			FieldVector<BigFraction> probsGo = probs.preMultiply(initial);
			probsGo = probsGo.add(goals);

			FieldVector<BigFraction> timesGo = times.preMultiply(initial);

			FieldVector<BigFraction> ones = new ArrayFieldVector<>(numberOfGlobalOptima, BigFraction.ONE);
			BigFraction val = ones.dotProduct(probsGo);
			
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
			probabilityOfHittingGlobalOptima = new BigFraction[numberOfGlobalOptima];
			//Arrays.fill(probabilityOfHittingGlobalOptima, null);
			
			expectedHittingTimeIfGlobalOptimaReached = new BigFraction [numberOfGlobalOptima];
			//Arrays.fill(expectedHittingTimeIfGlobalOptimaReached, Double.NaN);
		}

	}


	private void computeStationaryProbability() {
		int numberOfLocalOptima = basin.length;
		
		Array2DRowFieldMatrix<BigFraction> matrix = new Array2DRowFieldMatrix<BigFraction>(BigFractionField.getInstance(), 
				numberOfLocalOptima, 
				numberOfLocalOptima);
		
		FieldVector<BigFraction> constant = new ArrayFieldVector<>(BigFractionField.getInstance(),numberOfLocalOptima);
		constant.setEntry(numberOfLocalOptima-1, BigFraction.ONE);
		for (int i=0; i < numberOfLocalOptima-1; i++) {
			for (int j=0; j < numberOfLocalOptima; j++) {
				matrix.setEntry(i, j, transitionMatrix[j][i].subtract(delta(i,j)));
			}
		}
		for (int j=0; j < numberOfLocalOptima; j++) {
			matrix.setEntry(numberOfLocalOptima-1, j, BigFraction.ONE);
		}

		try {
			FieldDecompositionSolver<BigFraction> solver = new FieldLUDecomposition<BigFraction>(matrix).getSolver();
			FieldVector<BigFraction> result = solver.solve(constant);
			stationaryDistribution = result.toArray();
		} catch (SingularMatrixException e) {
			// FIXME: I am assuming here that the Markov chain is reducible
			stationaryDistribution = new BigFraction [numberOfLocalOptima];

		}		
	}

	private void writeTransitionMatrix(Formatter formatter) {
		formatter.format("LO from\tLO to\tProbability\n");
		int numberOfLocalOptima = fitness.length;
		for (int x=0; x < numberOfLocalOptima; x++) {
			for (int y=0; y < numberOfLocalOptima; y++) {
				if (doubleOutput) {
					formatter.format("%d\t%d\t%f\n",x,y,transitionMatrix[x][y].doubleValue());
				} else {
					formatter.format("%d\t%d\t%s\n",x,y,transitionMatrix[x][y]);
				}
				
			}
		}
	}
	
	private double nullableBigFraction(BigFraction bf) {
		return (bf==null)?Double.NaN:bf.doubleValue(); 
		
	}

	private void writeLocalOptimaInformation(Formatter formatter) {
		formatter.format("LO\tFitness\tSize of basin of attraction\tExpected hitting time\tStationary distribution\tCommunicating component\n");
		int numberOfLocalOptima = fitness.length;
		for (int lo=0; lo < numberOfLocalOptima; lo++) {
			if (doubleOutput) {
				formatter.format("%d\t%f\t%d\t%f\t%f\t%d\n",lo,fitnessValue[fitness[lo]],basin[lo],
						nullableBigFraction(expectedHittingTime[lo]), 
						nullableBigFraction(stationaryDistribution[lo]), 
						component[lo]);
			} else {
				formatter.format("%d\t%f\t%d\t%s\t%s\t%d\n",lo,fitnessValue[fitness[lo]],basin[lo],expectedHittingTime[lo], stationaryDistribution[lo], component[lo]);
			}
		}
	}

	private void writeGlobalOptimaInformation(Formatter formatter) {
		formatter.format("GO\tHitting probability\tExpected hitting time if reached\n");
		int goStartIndex = fitness.length-numberOfGlobalOptima;
		for (int go=0; go < numberOfGlobalOptima; go++) {
			if (doubleOutput) {
				formatter.format("%d\t%f\t%f\n",go+goStartIndex,nullableBigFraction(probabilityOfHittingGlobalOptima[go]), nullableBigFraction(expectedHittingTimeIfGlobalOptimaReached[go]));
			} else {
				formatter.format("%d\t%s\t%s\n",go+goStartIndex,probabilityOfHittingGlobalOptima[go], expectedHittingTimeIfGlobalOptimaReached[go]);
			}
			
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
		transitionMatrix = new BigFraction[numberOfLocalOptima][numberOfLocalOptima];
		for (int x=0; x < numberOfLocalOptima; x++) {
			for (int y=0; y < numberOfLocalOptima; y++) {
				transitionMatrix[x][y] = BigFraction.ZERO;
			}
		}

		for (int d=0; d <= n; d++) {
			BigFraction probability = distribution.getProbability(d);
			if (probability.equals(BigFraction.ZERO)) {
				continue;
			}

			for (int x=0; x < numberOfLocalOptima; x++) {
				for (int y=0; y < numberOfLocalOptima; y++) {
					BigFraction intermediate = probability.multiply(BigInteger.valueOf(markovStrategyArray[markovSampleCoordinates(x, d, y)]));
					transitionMatrix[x][y] = transitionMatrix[x][y].add(intermediate);
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
			if (transitionMatrix[v][w].compareTo(BigFraction.ZERO)>0) {
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


}
