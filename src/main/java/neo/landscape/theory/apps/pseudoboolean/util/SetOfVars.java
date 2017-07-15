package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.HashSet;

public class SetOfVars extends HashSet<Integer> {

	protected SetOfVars(int[] v) {
		for (int i : v) {
            add(i);
        }
	}

	public static SetOfVars immutable(int... v) {
		return new SetOfVars(v);
	}

	public SetOfVars() {
		super();
	}

	public void inmute() {
	}
}