package neo.landscape.theory.apps.pseudoboolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import neo.landscape.theory.apps.util.UnionFind;

public class UnionFindTest {
	
	private Random rnd = new Random(0);
	
	@Test
	public void testUnionFindForIntegers() {
		UnionFind<Integer> unionFind = UnionFind.basicImplementation();
		
		Stream.of(3,4,5,6)
			.forEach(e->unionFind.makeSet(e));
		
		unionFind.union(3, 5);
		
		
		assertEquals(3, unionFind.getNumberOfSets());
		assertEquals(unionFind.findSet(3),unionFind.findSet(5));
		assertFalse(unionFind.findSet(4).equals(unionFind.findSet(3)));
		Set<Integer> set = Stream.of(3,4,5,6)
			.map(e->unionFind.findSet(e))
			.collect(Collectors.toSet());
		
		assertEquals(3, set.size());
		
	}
	
	@Test
	public void testUnionFindStreamForIntegers() {
		UnionFind<Integer> unionFind = UnionFind.basicImplementation();
		
		Stream.of(3,4,5,6)
			.forEach(e->unionFind.makeSet(e));
		
		unionFind.union(3, 5);
		unionFind.union(5, 4);
		
		assertEquals(3,unionFind.elementsInSameSetAs(3).count());
		
	}
	
	private PBSolution generateRandomSolution(int n) {
		PBSolution solution = new PBSolution(n);
		for (int i=0; i < n; i++) {
			if (rnd.nextBoolean()) {
				solution.setBit(i, 1);
			}
		}
		return solution;
	}
	
	@Test
	public void testUnionFindForPBSolutions() {
		UnionFind<PBSolution> unionFind = UnionFind.basicImplementation();
		
		PBSolution [] sols = {
				generateRandomSolution(10),
				generateRandomSolution(10),
				generateRandomSolution(10),
				generateRandomSolution(10),
				generateRandomSolution(10)};
		
		Stream.of(sols)
			.forEach(e->unionFind.makeSet(e));
		
		unionFind.union(sols[2], sols[4]);
		
		
		assertEquals(4, unionFind.getNumberOfSets());
		assertEquals(unionFind.findSet(sols[2]),unionFind.findSet(sols[4]));
		assertFalse(unionFind.findSet(sols[4]).equals(unionFind.findSet(sols[3])));
		Set<PBSolution> set = Stream.of(sols)
			.map(e->unionFind.findSet(e))
			.collect(Collectors.toSet());
		
		assertEquals(4, set.size());
		
	}

}
