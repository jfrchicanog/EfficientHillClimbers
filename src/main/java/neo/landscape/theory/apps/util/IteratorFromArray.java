package neo.landscape.theory.apps.util;

import java.util.Iterator;

public class IteratorFromArray {

	public static Iterator<Integer> iterator(final int[] var) {
		return new Iterator<Integer>() {
			int ind = 0;

			@Override
			public boolean hasNext() {
				return ind < var.length;
			}

			@Override
			public Integer next() {
				return var[ind++];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	public static Iterable<Integer> iterable(final int[] var) {
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return IteratorFromArray.iterator(var);
			}

		};
	}

	public static <T> Iterable<T> iterable(final T[] var) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return IteratorFromArray.iterator(var);
			}
		};
	}

	public static <T> Iterator<T> iterator(final T[] var) {
		return new Iterator<T>() {
			int ind = 0;

			@Override
			public boolean hasNext() {
				return ind < var.length;
			}

			@Override
			public T next() {
				return var[ind++];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

}
