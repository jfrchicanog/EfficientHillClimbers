package neo.landscape.theory.apps.pseudoboolean.util;

public class DisjointSetArrays implements DisjointSets {
	private int [] parent;
	private int [] rank;
	
	public DisjointSetArrays(int [] parent, int [] rank) {
		this.parent = parent;
		this.rank = rank;
	}

	@Override
	public void makeSet(int x) {
		parent[x] = x;
		rank[x] = 0;
	}

	@Override
	public int findSet(int x) {
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
		if (rank[x] < rank[y]) {
			parent[x] = y;
		} else {
			parent[y] = x;
			if (rank[x] == rank[y]) {
				rank[y]++;
			}
		}
	}

}
