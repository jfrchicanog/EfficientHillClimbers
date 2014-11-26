package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import neo.landscape.theory.apps.util.IteratorFromArray;

public class SetOfVars extends BitSet implements Iterable<Integer> {

	public int[] vars = null;

	protected SetOfVars(int[] v) {
		vars = v;
	}

	public static SetOfVars immutable(int... v) {
		return new SetOfVars(v);
	}

	public SetOfVars() {
		super();
	}

	public void inmute() {
		int[] aux = new int[cardinality()];
		int j = 0;
		for (int v : this) {
			aux[j++] = v;
		}
		vars = aux;
	}

	public void add(int i) {
		if (vars == null) {
			set(i);
		} else {
			throw new RuntimeException(
					"Cannot add more variables if the SetOfVars is inmmutable");
		}

	}

	public boolean contains(int i) {
		if (vars != null) {
			for (int j = 0; j < vars.length; j++) {
				if (vars[j] == i) {
					return true;
				}
			}
			return false;

		} else {
			return get(i);
		}
	}

	public int size() {
		if (vars != null) {
			return vars.length;
		} else {
			return cardinality();
		}
	}

	@Override
	public int hashCode() {
		if (vars != null) {
			return Arrays.hashCode(vars);
		} else {
			return super.hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (vars != null && obj instanceof SetOfVars
				&& ((SetOfVars) obj).vars != null)
			return Arrays.equals(vars, ((SetOfVars) obj).vars);
		return super.equals(obj);

	}

	@Override
	public Iterator<Integer> iterator() {
		if (vars != null) {
			return IteratorFromArray.iterator(vars);
		} else {
			return new Iterator<Integer>() {
				int var = nextSetBit(0);

				@Override
				public boolean hasNext() {
					return var != -1;
				}

				@Override
				public Integer next() {
					int res = var;
					if (res < 0) {
						throw new NoSuchElementException();
					} else {
						var = nextSetBit(var + 1);
					}

					return res;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

			};
		}
	}
}