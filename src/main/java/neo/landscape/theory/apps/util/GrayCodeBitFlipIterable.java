package neo.landscape.theory.apps.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class GrayCodeBitFlipIterable implements Iterable<Integer> {

	private static class GrayCodeBitFlipIterator implements Iterator<Integer> {
		private GrayCodeBitFlipIterator it;
		private boolean second;
		private int n;

		public GrayCodeBitFlipIterator(int n) {
			this.n = n;
			if (n < 1) {
				throw new IllegalArgumentException("n should be at least 1");
			}

			second = false;
			if (n > 1) {
				it = new GrayCodeBitFlipIterator(n - 1);
			}
		}

		@Override
		public boolean hasNext() {
			if (!second)
				return true;
			if (it != null)
				return it.hasNext();
			return false;
		}

		@Override
		public Integer next() {

			if (second) {
				if (it != null) {
					return it.next();
				} else {
					throw new NoSuchElementException();
				}
			} else {
				if (it != null) {
					if (it.hasNext()) {
						return it.next();
					} else {
						second = true;
						it = new GrayCodeBitFlipIterator(n - 1);
						return n - 1;
					}
				} else {
					second = true;
					return n - 1;
				}
			}

		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private int n;

	public GrayCodeBitFlipIterable(int n) {
		this.n = n;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Arguments: <n>");
		}
		for (int i : new GrayCodeBitFlipIterable(Integer.parseInt(args[0]))) {
			System.out.println(i);
		}

	}

	@Override
	public Iterator<Integer> iterator() {
		return new GrayCodeBitFlipIterator(n);
	}

}
