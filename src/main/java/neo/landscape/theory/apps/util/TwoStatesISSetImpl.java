package neo.landscape.theory.apps.util;

import java.util.HashSet;
import java.util.Set;

public class TwoStatesISSetImpl implements TwoStatesIntegerSet {

	private Set<Integer> explored;
	private Set<Integer> unexplored;

	public TwoStatesISSetImpl(int n) {
		explored = new HashSet<Integer>();
		unexplored = new HashSet<Integer>();

		for (int i = 0; i < n; i++) {
			unexplored.add(i);
		}
	}

	@Override
	public void reset() {
		unexplored.addAll(explored);
		explored.clear();
	}

	@Override
	public int getNextUnexplored() {
		return unexplored.iterator().next();
	}

	@Override
	public boolean hasMoreUnexplored() {
		return unexplored.iterator().hasNext();
	}

	private void checkRange(int v) {
		if (v < 0 || v >= getNumberOfElements()) {
			throw new IllegalArgumentException(
					"This integer does not exist in the set");
		}
	}

	@Override
	public void explored(int v) {
		checkRange(v);
		explored.add(v);
		unexplored.remove(v);
	}

	@Override
	public void unexplored(int v) {
		checkRange(v);
		unexplored.add(v);
		explored.add(v);
	}

	@Override
	public boolean isExplored(int v) {
		checkRange(v);
		return explored.contains(v);
	}

	@Override
	public int getNumberOfElements() {
		return explored.size() + unexplored.size();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

    @Override
    public int getNumberOfExploredElements() {
        return explored.size();
    }

}
