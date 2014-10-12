package neo.landscape.theory.apps.pseudoboolean;


public class ParseResults {
	
	private static final String PARSERS_PACKAGE = "neo.landscape.theory.apps.pseudoboolean.parsers";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ProcessManager(PARSERS_PACKAGE).execute(args);
	}

}
