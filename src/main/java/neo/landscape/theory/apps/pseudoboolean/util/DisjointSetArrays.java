package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.NoSuchElementException;

public class DisjointSetArrays implements DisjointSets {
	private int [] parent;
	private int [] rank;
	private int numberOfSets;
	
	public DisjointSetArrays(int [] parent, int [] rank) {
		this.parent = parent;
		this.rank = rank;
		clear();
	}

	@Override
	public void makeSet(int x) {
		if (rank[x] < 0) {
			parent[x] = x;
			rank[x] = 0;
			numberOfSets++;
		}
	}

	@Override
	public int findSet(int x) {
		if (rank[x] < 0) {
			throw new NoSuchElementException();
		}
		int v = x;
		while (parent[v]!=v) {
			v = parent[v];
		}
		while (parent[x]!=v) {
			int t = parent[x];
			parent[x] = v;
			x = t;
		}
		return v;
	}

	@Override
	public void union(int x, int y) {
		x = findSet(x);
		y = findSet(y);
		if (x != y) {
			if (rank[x] < rank[y]) {
				parent[x] = y;
			} else {
				parent[y] = x;
				if (rank[x] == rank[y]) {
					rank[y]++;
				}
			}
			numberOfSets--;
		}
	}

	@Override
	public int getNumberOfSets() {
		return numberOfSets;
	}

	@Override
	public void clear() {
		numberOfSets=0;
		for (int i=0; i < rank.length; i++) {
			rank[i]=-1;
		}
	}

}
