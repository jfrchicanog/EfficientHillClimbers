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

	public static <S, T extends S> Iterable<S> iterable(final T[] var) {
		return new Iterable<S>() {
			public Iterator<S> iterator() {
				return IteratorFromArray.iterator(var);
			}
		};
	}
	
	public static <S, T extends S> Iterable<S> iterable(final T[] var, final int start, final int end) {
        return new Iterable<S>() {
            public Iterator<S> iterator() {
                return IteratorFromArray.iterator(var, start, end);
            }
        };
    }

	public static <S, T extends S> Iterator<S> iterator(final T[] var) {
		return iterator(var, 0, var.length);
	}
	
	public static <S, T extends S> Iterator<S> iterator(final T[] var, final int start, final int end) {
        return new Iterator<S>() {
            int ind = start;

            @Override
            public boolean hasNext() {
                return ind < end;
            }

            @Override
            public S next() {
                return var[ind++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

}
