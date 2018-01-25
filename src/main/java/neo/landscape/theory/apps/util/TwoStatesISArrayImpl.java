package neo.landscape.theory.apps.util;

import java.util.NoSuchElementException;

public class TwoStatesISArrayImpl implements TwoStatesIntegerSet {

	protected int[] variable;
	protected int[] position;
	protected int next;

	public TwoStatesISArrayImpl(int n) {
		variable = new int[n];
		position = new int[n];

		for (int i = 0; i < variable.length; i++) {
			variable[i] = position[i] = i;
		}
		next = 0;
	}

	@Override
	public void reset() {
		next = 0;
	}

	@Override
	public int getNextUnexplored() {
		if (next >= variable.length) {
			throw new NoSuchElementException();
		}
		return variable[next];
	}

	@Override
	public boolean hasMoreUnexplored() {
		return next < variable.length;
	}

	private void swapVarsAt(int p1, int p2) {
		int v1 = variable[p1];
		int v2 = variable[p2];

		variable[p1] = v2;
		variable[p2] = v1;

		position[v1] = p2;
		position[v2] = p1;
	}

	@Override
	public void explored(int v) {
		checkRange(v);
		if (!isExplored(v)) {
			swapVarsAt(next, position[v]);
			next++;
		}
	}

	@Override
	public void unexplored(int v) {
		checkRange(v);
		if (isExplored(v)) {
			next--;
			swapVarsAt(next, position[v]);
		}
	}

	private void checkRange(int v) {
		if (v < 0 || v >= position.length) {
			throw new IllegalArgumentException(
					"This integer does not exist in the set");
		}
	}

	@Override
	public boolean isExplored(int v) {
		checkRange(v);
		return position[v] < next;
	}

	@Override
	public int getNumberOfElements() {
		return variable.length;
	}

    @Override
    public int getNumberOfExploredElements() {
        return next;
    }

}
