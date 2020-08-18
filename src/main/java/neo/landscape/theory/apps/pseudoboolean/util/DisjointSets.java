package neo.landscape.theory.apps.pseudoboolean.util;

public interface DisjointSets {
	int getNumberOfSets();
	void clear();
	void makeSet(int x);
	int findSet(int x);
	void union(int x, int y);
	default boolean sameSet(int x, int y) {
		return findSet(x) == findSet(y);
	}
}
