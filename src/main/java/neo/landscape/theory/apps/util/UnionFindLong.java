package neo.landscape.theory.apps.util;

import java.util.OptionalLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface UnionFindLong {
	void clear();
	int getNumberOfSets();
	void union (long e1, long e2);
	long findSet(long e);
	OptionalLong findSetIfContained(long e);
	void makeSet(long e);
	boolean makeSetIfNotContained(long e);
	boolean contains(long e);
	boolean sameSet(long e1, long e2);
	LongStream elementsInSameSetAs(long e);
	LongStream canonicalRepresentatives();
	
	static UnionFindLong basicImplementation(long n) {
		return new UnionFindLongBasicImplementation(n);
	}
}
