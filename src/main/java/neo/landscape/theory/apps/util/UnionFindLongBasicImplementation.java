package neo.landscape.theory.apps.util;

import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.stream.LongStream;

public class UnionFindLongBasicImplementation implements UnionFindLong {
	private int [] parent;
	private int [] rank;
	private int numberOfSets;
	
	public UnionFindLongBasicImplementation(long n) {
		parent = new int [(int)n];
		rank = new int [(int)n];
		clear();
	}

	@Override
	public void clear() {
		numberOfSets=0;
		for (int i=0; i < parent.length; ++i) {
			rank[i]=-1;
		}
	}

	@Override
	public int getNumberOfSets() {
		return numberOfSets;
	}

	@Override
	public void union(long x, long y) {
		x = findSet(x);
		y = findSet(y);
		int ix = (int)x;
		int iy = (int)y;
		if (ix!=iy) {
			if (rank[ix] < rank[iy]) {
				parent[ix] = iy;
			} else {
				parent[iy] = ix;
				if (rank[ix]==rank[iy]) {
					++rank[iy];
				}
			}
			numberOfSets--;
		}
	}

	@Override
	public long findSet(long e) {
		int ie = (int)e;
		if (rank[ie] < 0) {
			throw new NoSuchElementException();
		}
		
		int v = ie;
		while (parent[v]!=v) {
			v = parent[v];
		}
		while (parent[ie]!=v) {
			int t = parent[ie];
			parent[ie] = v;
			ie = t;
		}
		return v;
	}

	@Override
	public void makeSet(long e) {
		int ie = (int)e;
		if (rank[ie] < 0) {
			parent[ie] = ie;
			rank[ie]=0;
			numberOfSets++;
		}
	}

	@Override
	public boolean contains(long e) {
		return rank[(int)e] >= 0;
	}

	@Override
	public boolean sameSet(long e1, long e2) {
		return findSet(e1)==findSet(e2);
	}

	@Override
	public LongStream elementsInSameSetAs(long e) {
		return LongStream.range(0, rank.length)
			.filter(this::contains)
			.filter(e1->sameSet(e,e1));
	}

	@Override
	public OptionalLong findSetIfContained(long e) {
		return contains(e)?OptionalLong.of(findSet(e)):OptionalLong.empty();
	}

	@Override
	public boolean makeSetIfNotContained(long e) {
		if (contains(e)) {
			return false;
		}
		makeSet(e);
		return true;
	}

	@Override
	public LongStream canonicalRepresentatives() {
		return LongStream.range(0, parent.length)
			.filter(l->rank[(int)l] >=0 && parent[(int)l]==l);
	}

}
