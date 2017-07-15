package neo.landscape.theory.apps.efficienthc;

import java.math.BigDecimal;

public interface SingleobjectiveProblem extends Problem {
	public <P extends SingleobjectiveProblem> double evaluate(Solution<P> sol);
	public <P extends SingleobjectiveProblem> BigDecimal evaluateArbitraryPrecision(
			Solution<P> sol);
}
