package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo.constrained;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import neo.landscape.theory.apps.efficienthc.Solution;
import neo.landscape.theory.apps.efficienthc.mo.MultiobjectiveHillClimberForInstanceOf;
import neo.landscape.theory.apps.efficienthc.mo.MultiobjectiveHillClimberSnapshot;
import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.MovesAndSubFunctionInspectorFactory;
import neo.landscape.theory.apps.pseudoboolean.problems.EmbeddedLandscape;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.VectorMKLandscape;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfSetOfVars;
import neo.landscape.theory.apps.pseudoboolean.util.SetOfVars;
import neo.landscape.theory.apps.util.IteratorFromArray;
import neo.landscape.theory.apps.util.RootedTreeGenerator;
import neo.landscape.theory.apps.util.RootedTreeGenerator.RootedTreeCallback;

public class MultiObjectiveConstrainedHammingBallHillClimberForInstanceOf implements
		MultiobjectiveHillClimberForInstanceOf<VectorMKLandscape> {

	private interface RecursiveCalls {
		public void run(int depth);
	}

	protected MultiObjectiveConstrainedHammingBallHillClimber rball;
	protected VectorMKLandscape problem;
	
	protected int[][] subfns;
	protected int[] oneFlipScores;
	private int[] variables;
	private int[] indices;
	private long problemInitTime;
	protected Properties configuration;
	protected Map<SetOfVars, Integer> minimalPerfectHash;
	protected int max_k;
	private SetOfVars affectedSubfns;

	public MultiObjectiveConstrainedHammingBallHillClimberForInstanceOf(
			MultiObjectiveConstrainedHammingBallHillClimber rball, VectorMKLandscape problem) {
		this.rball = rball;
		this.problem = problem;
		initializeOperatorDependentStructures();
		initializeProblemDependentStructures();
	}

	private void initializeOperatorDependentStructures() {
		variables = new int[rball.radius + 1];
		indices = new int[rball.radius + 1];
	}

	private void initializeProblemDependentStructures() {
		long init = System.currentTimeMillis();
		minimalPerfectHash = new HashMap<SetOfVars, Integer>();
		int n = problem.getN();
		int m = problem.getSubfunctionsTranslator().getM();
		max_k = 0;
		subfns = new int[m][];
		oneFlipScores = new int[n];

		Set<Integer>[] aux_var_combs = computeMinimalPerfectHashAndOneFlipScores();

		Set<Integer> set = new HashSet<Integer>();

		for (int sf = 0; sf < subfns.length; sf++) {
			set.clear();
			int maskLength = problem.getMaskLength(sf);
			if (maskLength > max_k) {
				max_k = maskLength;
			}

			int limit = maskLength;
			for (int v = 0; v < limit; v++) {
				set.addAll(aux_var_combs[problem.getMasks(sf, v)]);
			}

			subfns[sf] = new int[set.size()];

			int j = 0;
			for (int e : set) {
				subfns[sf][j++] = e;
			}
		}

		affectedSubfns = new SetOfVars();
		problemInitTime = System.currentTimeMillis() - init;
	}

    protected Set<Integer>[] computeMinimalPerfectHashAndOneFlipScores() {
        int n=problem.getN();
        Set<Integer>[] scoresPerVariable = new HashSet[n];

		for (int variable = 0; variable < scoresPerVariable.length; variable++) {
			scoresPerVariable[variable] = new HashSet<Integer>();
		}

		for (int variable = 0; variable < n; variable++) {
			SetOfSetOfVars moves = generateTuples(variable);
			for (SetOfVars move : moves) {
				Integer moveID = addMoveToMinimalPerfectHashIfNecessary(variable, move);
				for (int vv : move) {
					scoresPerVariable[vv].add(moveID);
				}
			}
		}
        return scoresPerVariable;
    }

    protected Integer addMoveToMinimalPerfectHashIfNecessary(int variable, SetOfVars move) {
        Integer moveID = minimalPerfectHash.get(move);
        if (moveID == null) {
        	moveID = minimalPerfectHash.size();
        	minimalPerfectHash.put(move, moveID);
        	if (move.size() == 1) {
        		oneFlipScores[variable] = moveID;
        	}
        }
        return moveID;
    }

	protected Iterable<Integer> subFunctionsAffected(SetOfVars bits) {
		Iterable<Integer> sfs;

		if (bits.size() == 1) {
			int bit = bits.iterator().next();
			sfs = IteratorFromArray.iterable(problem.getAppearsIn()[bit]);
		} else {
			affectedSubfns.clear();
			for (int bit : bits) {
				for (int sf : problem.getAppearsIn()[bit]) {
					affectedSubfns.add(sf);
				}
			}
			sfs = affectedSubfns;
		}
		return sfs;
	}

	private SetOfSetOfVars generateTuples(final int v) {
		final SetOfSetOfVars ssv = new SetOfSetOfVars();
		RootedTreeGenerator rtg = new RootedTreeGenerator();

		// Generate all the trees up to r and for each tree do
		rtg.generate(rball.radius, new RootedTreeCallback() {
			int[][] interactions = problem.getInteractions();

			@Override
			public void rootedTree(final int[] par, final int p) {
				// Navigate over the variables of the subfunction and plug-in
				// the variables in the tree
				// Now, plug the variables in the tree (array "variables")

				if (p == 1) {
					SetOfVars sov = new SetOfVars();
					sov.add(v);
					sov.inmute();
					ssv.add(sov);
					return;
				}
				// else

				variables[1] = v;

				new RecursiveCalls() {
					@Override
					public void run(int depth) {
						int par_var;
						int[] src;

						indices[depth] = 0;

						par_var = variables[par[depth]];
						src = interactions[par_var];

						for (; indices[depth] < src.length; indices[depth]++) {
							variables[depth] = src[indices[depth]];
							// Check if it less than the first one
							if (variables[depth] <= v) {
								continue;
							}
							// Check if the variable appeared before
							int ptr = 1;
							while (ptr < depth
									&& variables[ptr] != variables[depth]) {
								ptr++;
							}

							if (ptr < depth) {
								// The variable appeared, we have to continue
								// with the next variable
								continue;
							}
							// else the variable didn't appear so we follow in
							// the body
							// Check if we assigned all the variables
							if (depth == p) {
								// If this is the case, we have to mark the set
								// of variables as "to be updated"
								// For each combination, mark the sets of
								// variables as "touched"
								SetOfVars sov = new SetOfVars();
								for (int var = 1; var <= p; var++) {
									sov.add(variables[var]);
								}
								sov.inmute();
								ssv.add(sov);
							} else {
								run(depth + 1);
							}
						}
					}
				}.run(2);
			}
		});
		return ssv;
	}


	public int getStoredScores() {
		return minimalPerfectHash.size();
	}

	public long getProblemInitTime() {
		return problemInitTime;
	}

	@Override
	public MultiObjectiveConstrainedHammingBallHillClimber getHillClimber() {
		return rball;
	}

	@Override
	public VectorMKLandscape getProblem() {
		return problem;
	}

    @Override
    public MultiObjectiveConstrainedHammingBallHillClimberSnapshot initialize(double [] weights, Solution<?> sol) {
        if (sol instanceof PBSolution) {
            return new MultiObjectiveConstrainedHammingBallHillClimberSnapshot(this, weights, (PBSolution) sol);
        } else {
            throw new IllegalArgumentException(
                    "Expected argument of class PBSolution but found "
                            + sol.getClass().getCanonicalName());
        }
    }

}
