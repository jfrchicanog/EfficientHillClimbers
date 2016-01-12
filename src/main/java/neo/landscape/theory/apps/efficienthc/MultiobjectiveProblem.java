package neo.landscape.theory.apps.efficienthc;

import java.math.BigDecimal;
import java.util.Properties;

public interface MultiobjectiveProblem extends Problem{
	public <P extends MultiobjectiveProblem> double [] evaluate(Solution<P> sol);
	public <P extends MultiobjectiveProblem> BigDecimal [] evaluateArbitraryPrecision(
			Solution<P> sol);
}
