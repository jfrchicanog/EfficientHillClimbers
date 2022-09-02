package neo.landscape.theory.apps.util;

import java.util.Optional;
import java.util.stream.Stream;

public interface UnionFind<E> {
	void clear();
	int getNumberOfSets();
	void union (E e1, E e2);
	E findSet(E e);
	Optional<E> findSetIfContained(E e);
	void makeSet(E e);
	boolean makeSetIfNotContained(E e);
	boolean contains(E e);
	boolean sameSet(E e1, E e2);
	Stream<E> elementsInSameSetAs(E e);
	Stream<E> canonicalRepresentatives();
	
	static <E> UnionFind<E> basicImplementation() {
		return new UnionFindBasicImplementation<>();
	}
}
