package neo.landscape.theory.apps.pseudoboolean;

import neo.landscape.theory.apps.util.ProcessManager;

public class Experiments {

	private static final String EXPERIMENTS_PACKAGE = "neo.landscape.theory.apps.pseudoboolean.experiments";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ProcessManager(EXPERIMENTS_PACKAGE).execute(args);
	}

}
