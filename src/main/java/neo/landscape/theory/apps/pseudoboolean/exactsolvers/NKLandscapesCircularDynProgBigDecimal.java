package neo.landscape.theory.apps.pseudoboolean.exactsolvers;

import java.math.BigDecimal;

import neo.landscape.theory.apps.efficienthc.ExactSolutionMethod;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.problems.NKLandscapes;

public class NKLandscapesCircularDynProgBigDecimal implements
		ExactSolutionMethod<NKLandscapes> {

	private BigDecimal[][] subfunctions;
	private BigDecimal[][] u; // Wright et al.'s u temporary array
	private BigDecimal[][] v; // Wright et al.'s v temporary array
	private int n;
	private int K;
	private int r;
	private NKLandscapes problem;

	@Override
	public SolutionQuality<NKLandscapes> solveProblem(NKLandscapes problem) {

	    if (!problem.isCircular()) {
            throw new IllegalArgumentException("The instance is not circular");
        }
	    
		this.problem = problem;
		K = problem.getK() - 1;

		n = problem.getN();
		if (K == 0) {
			adaptSubfunctions(problem.getSubFunctions());
			// Linear problem, solve it directly
			return solveLinear();
		} else if (K == 1) {
			adaptSubfunctions(problem.getSubFunctions());
			r = K;
		} else if ((n % K) != 0 && n < 2 * K) {
			CompleteEnumeration ce = new CompleteEnumeration();
			return ce.solveProblem(problem);
		} else {
			computeSubfunctions();
		}

		return solveWright();
	}

	private void adaptSubfunctions(double[][] subfns) {
		subfunctions = new BigDecimal[subfns.length][];
		for (int i = 0; i < subfns.length; i++) {
			subfunctions[i] = new BigDecimal[subfns[i].length];
			for (int j = 0; j < subfns[i].length; j++) {
				subfunctions[i][j] = new BigDecimal(subfns[i][j]);
			}
		}
	}

	private void computeSubfunctions() {
		r = n % K;
		int number = n / K;
		if (r > 0) {
			number++;
		} else {
			r = K;
		}

		double[][] orig_subfn = problem.getSubFunctions();

		int limit_b = 1 << K;

		subfunctions = new BigDecimal[number][];
		int fn = 0;
		for (int f = 0; f < number; f++) {
			int shift_a = K;
			if (f == 1) {
				shift_a = r;
			}

			int limit_a = 1 << shift_a;
			subfunctions[f] = new BigDecimal[1 << (K + shift_a)];

			for (int a = 0; a < limit_a; a++) {
				for (int b = 0; b < limit_b; b++) {
					BigDecimal val = BigDecimal.ZERO;
					int shift = (b << shift_a) + a;

					for (int j = 0; j < shift_a; j++) {
						val = val.add(new BigDecimal(orig_subfn[fn + j][shift
								& ((1 << (K + 1)) - 1)]));
						shift >>>= 1;
					}

					subfunctions[f][(b << shift_a) + a] = val;
				}
			}

			fn += shift_a;
		}

	}

	private BigDecimal readFn(int i, int a, int b) {
		int index = (b << K) + a;
		return subfunctions[i][index];
	}

	private void writeFn(int i, int a, int b, BigDecimal v) {
		int index = (b << K) + a;
		subfunctions[i][index] = v;
	}

	private SolutionQuality<NKLandscapes> solveWright() {
		// Wright et al.'s algorithm (IEEE TEVC 4(4):373, 2000)

		final int limit = 1 << K;
		u = new BigDecimal[limit][limit];
		v = new BigDecimal[limit][limit];

		for (int f = subfunctions.length - 1; f >= (r < K ? 3 : 2); f--) {
			for (int a = 0; a < limit; a++)
				for (int c = 0; c < limit; c++) {
					u[a][c] = null;
					for (int b = 0; b < limit; b++) {
						BigDecimal sum = readFn(f - 1, a, b).add(
								readFn(f, b, c));
						if (u[a][c] == null || sum.compareTo(u[a][c]) > 0) {
							u[a][c] = sum;
							v[a][c] = new BigDecimal(b);
						}
					}
				}

			for (int a = 0; a < limit; a++)
				for (int c = 0; c < limit; c++) {
					writeFn(f, a, c, v[a][c]);
					writeFn(f - 1, a, c, u[a][c]);
				}
		}

		// Solve he rest exhaustively
		int amax = 0;
		int cmax = 0;
		int bmax = 0;
		BigDecimal max = null;
		PBSolution pbsol = new PBSolution(n);

		if (r == K) {
			for (int a = 0; a < limit; a++)
				for (int c = 0; c < limit; c++) {
					BigDecimal val = readFn(0, a, c).add(readFn(1, c, a));
					if (max == null || val.compareTo(max) > 0) {
						max = val;
						amax = a;
						cmax = c;
					}
				}
		} else {
			int limit_r = 1 << r;
			for (int a = 0; a < limit; a++)
				for (int b = 0; b < limit_r; b++)
					for (int c = 0; c < limit; c++) {
						int index1 = (c << r) + b;
						int index0 = ((index1 << K) + a) & ((1 << (2 * K)) - 1);
						int index2 = ((a << K) + c) & ((1 << (2 * K)) - 1);

						BigDecimal val = subfunctions[0][index0].add(
								subfunctions[1][index1]).add(
								subfunctions[2][index2]);
						if (max == null || val.compareTo(max) > 0) {
							max = val;
							amax = a;
							bmax = b;
							cmax = c;
						}
					}
		}

		SolutionQuality<NKLandscapes> sol = new SolutionQuality<NKLandscapes>();
		sol.solution = pbsol;
		// Warning: potential lack of precision
		sol.quality = max.doubleValue();
		// Should be better to log the values, instead of writing it to stdout
		//System.out.println("Arbitrary precision optimal value: "+max);
		// Build the solution

		int pos = 0;
		int start_fn = 2;
		setWithBits(pbsol, pos, K, amax);
		pos += K;
		if (r < K) {
			setWithBits(pbsol, pos, r, bmax);
			pos += r;
			start_fn = 3;
		}
		setWithBits(pbsol, pos, K, cmax);
		pos += K;

		int val = cmax;
		for (int f = start_fn; f < subfunctions.length; f++) {
			// Warning: potential lack of precision?
			val = readFn(f, val, amax).intValue();
			setWithBits(pbsol, pos, K, val);
			pos += K;
		}

		return sol;
	}

	private void setWithBits(PBSolution pbsol, int pos, int bits, int val) {
		for (int j = 0; j < bits; j++) {
			pbsol.setBit(pos + j, val & 0x01);
			val >>>= 1;
		}
	}

	private SolutionQuality<NKLandscapes> solveLinear() {
		SolutionQuality<NKLandscapes> sol = new SolutionQuality<NKLandscapes>();
		PBSolution pbsol = new PBSolution(n);
		sol.solution = pbsol;
		sol.quality = 0;
		for (int i = 0; i < n; i++) {
			int v;
			if (subfunctions[i][0].compareTo(subfunctions[i][1]) > 0) {
				v = 0;
			} else {
				v = 1;
			}
			pbsol.setBit(i, v);
			// Warning: potential lack of precision
			sol.quality += subfunctions[i][v].doubleValue();
		}

		return sol;
	}

	/**
	 * @param args
	 */

}
