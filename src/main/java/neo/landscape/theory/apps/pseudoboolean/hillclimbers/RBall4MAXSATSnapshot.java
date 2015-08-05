package neo.landscape.theory.apps.pseudoboolean.hillclimbers;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import neo.landscape.theory.apps.pseudoboolean.PBSolution;
import neo.landscape.theory.apps.pseudoboolean.hillclimbers.RBallEfficientHillClimberSnapshot.SubfunctionChangeListener;

public class RBall4MAXSATSnapshot extends RBallEfficientHillClimberSnapshot
		implements SubfunctionChangeListener {

	protected Set<Integer> unsatisfied;

	public RBall4MAXSATSnapshot(RBall4MAXSATForInstanceOf rballfio,
			PBSolution sol) {
		super(rballfio, sol);
		unsatisfied = new HashSet<Integer>();
		setSubfunctionChangeListener(this);
	}

	@Override
	public void valueChanged(int sf, double old_value, double new_value) {
		if (new_value == 0 && old_value != 0) {
			unsatisfied.add(sf);
		}

		if (new_value == 1 && old_value == 0) {
			unsatisfied.remove(sf);
		}

	}

	@Override
	public void softRestart(int soft_restart) {
		boolean tmp = collectFlips;
		collectFlips = false;

		Set<Integer> vars = new HashSet<Integer>();

		for (int un : unsatisfied) {
			int k = problem.getMaskLength(un);
			for (int v = 0; v < k; v++) {
				vars.add(problem.getMasks(un, v));
			}
		}

		int n = vars.size();

		for (int v : vars) {
			int r = rnd.nextInt(n);
			if (r < soft_restart) {
				RBallPBMove move = movesSelector.getMoveByID(rballfio.oneFlipScores[v]);
                solutionQuality += move.improvement;
				moveOneBit(v);

				soft_restart--;
			}
			n--;
		}

		collectFlips = tmp;
	}

}
