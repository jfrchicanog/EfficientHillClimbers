package neo.landscape.theory.apps.efficienthc;

import java.math.BigDecimal;
import java.util.Properties;

public interface Problem {
	public void setSeed(long seed);

	public void setConfiguration(Properties prop);

	public <P extends Problem> Solution<P> getRandomSolution();

	public <P extends Problem> double evaluate(Solution<P> sol);

	public <P extends Problem> BigDecimal evaluateArbitraryPrecision(
			Solution<P> sol);
}
