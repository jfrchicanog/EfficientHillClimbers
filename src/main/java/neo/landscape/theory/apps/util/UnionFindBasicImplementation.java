package neo.landscape.theory.apps.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnionFindBasicImplementation<E> implements UnionFind<E> {
	private Map<E,E> parent;
	private Map<E,Integer> rank;
	private int numberOfSets;
	
	public UnionFindBasicImplementation() {
		parent = new HashMap<>();
		rank = new HashMap<>();
	}

	@Override
	public void clear() {
		numberOfSets=0;
		rank.clear();
		parent.clear();
	}

	@Override
	public int getNumberOfSets() {
		return numberOfSets;
	}

	@Override
	public void union(E x, E y) {
		x = findSet(x);
		y = findSet(y);
		if (!x.equals(y)) {
			if (rank.get(x) < rank.get(y)) {
				parent.put(x, y);
			} else {
				parent.put(y, x);
				if (rank.get(x) == rank.get(y)) {
					rank.compute(y, (k,v)->v+1);
				}
			}
			numberOfSets--;
		}
		
	}

	@Override
	public E findSet(E e) {
		if (!rank.containsKey(e)) {
			throw new NoSuchElementException();
		}
		E v = e;
		while (!parent.get(v).equals(v)) {
			v = parent.get(v);
		}
		while (!parent.get(e).equals(v)) {
			E t = parent.get(e);
			parent.put(e,v);
			e = t;
		}
		return v;
	}

	@Override
	public void makeSet(E e) {
		if (!rank.containsKey(e)) {
			parent.put(e,e);
			rank.put(e,0);
			numberOfSets++;
		}
	}

	@Override
	public boolean contains(E e) {
		return rank.containsKey(e);
	}

	@Override
	public boolean sameSet(E e1, E e2) {
		return findSet(e1).equals(findSet(e2));
	}

	@Override
	public Stream<E> elementsInSameSetAs(E e) {
		return rank.keySet().stream().filter(e1->sameSet(e,e1));
	}

	@Override
	public Optional<E> findSetIfContained(E e) {
		return contains(e)?Optional.of(findSet(e)):Optional.empty();
	}

	@Override
	public boolean makeSetIfNotContained(E e) {
		if (contains(e)) {
			return false;
		}
		makeSet(e);
		return true;
	}

	@Override
	public Stream<E> canonicalRepresentatives() {
		return parent.entrySet().stream()
			.filter(entry->entry.getKey().equals(entry.getValue()))
			.map(entry->entry.getKey());
	}
}
