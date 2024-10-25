package neo.landscape.theory.apps.pseudoboolean;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PBSolutionTest {

	private Random rnd;

	@Test
	public void testPBSolution() {

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);
			for (int i = 0; i < n; i++) {
				assertEquals(0, pbs.getBit(i), "Not all bits set to zero at the beginning");
			}
		}
	}

	@Test
	public void testSetBit() {

		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);
			for (int i = 0; i < n; i++) {
				int v = rnd.nextInt(2);

				pbs.setBit(i, v);

				assertEquals(v, pbs.getBit(i), "setBit of getBit not working");
			}
		}
	}

	@Test
	public void testFlipBit() {

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);
			for (int i = 0; i < n; i++) {
				pbs.flipBit(i);
			}

			for (int i = 0; i < n; i++) {
				assertEquals(1, pbs.getBit(i), "flipBit not working");
			}

		}

	}

	@Test
	public void testRandomFlipBit() {

		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);

			for (int i = 0; i < n; i++) {
				int v = rnd.nextInt(2);
				pbs.setBit(i, v);
				pbs.flipBit(i);

				assertEquals(1 - v, pbs.getBit(i), "flipBit not working");

			}
		}

	}

	@Test
	public void testParse() {
		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);

			for (int i = 0; i < n; i++) {
				int v = rnd.nextInt(2);
				pbs.setBit(i, v);
			}

			PBSolution pos = new PBSolution(n);
			pos.parse(pbs.toString());

			for (int i = 0; i < n; i++) {
				assertEquals(pos.getBit(i), pbs.getBit(i), "Error in parse or toString");
			}

		}
	}

	@Test
	public void testEquals1() {
		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);
			PBSolution pbs2 = new PBSolution(n);
			for (int i = 0; i < n; i++) {
				int v = rnd.nextInt(2);

				pbs.setBit(i, v);
				pbs2.setBit(i, v);

			}
			assertEquals(pbs, pbs2, "equals not working");
		}
	}

	@Test
	public void testEquals2() {
		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);
			for (int i = 0; i < n; i++) {
				int v = rnd.nextInt(2);

				pbs.setBit(i, v);

			}
			PBSolution pbs2 = new PBSolution(pbs);
			assertEquals(pbs, pbs2, "equals not working");
		}
	}
	
	@Test
	public void testHexZero() {
	    PBSolution solution = new PBSolution(40);
	    assertEquals("Not the same", "0000000000000000", solution.toHex());
	}
	
	@Test
    public void testHexAllOnes() {
        PBSolution solution = new PBSolution(40);
        for (int i=0; i < 40; i++) {
            solution.flipBit(i);
        }
        assertEquals("Not the same", "000000ffffffffff", solution.toHex());
    }
	
	@Test
    public void testHexSome() {
        PBSolution solution = PBSolution.toPBSolution("1000111101010");
        assertEquals("Not the same", "000011ea", solution.toHex());
    }
	
	@Test
	public void fromHex() {
		rnd = new Random (0);
		for (int n: new Integer[] {4, 7, 9, 15, 16, 17, 23, 24, 25, 31, 32, 33, 63, 64, 65}) {
			for (int it=0; it < 10; it++) {
				PBSolution solution = generateRandomSolution (n);
				String hex = solution.toHex();
				PBSolution newSolution = new PBSolution(n);
				newSolution.fromHex(hex);
				assertEquals(solution, newSolution, "Solutions are not equal");
			}
		}
	}
	
	@Test
	public void testHamming() {
		rnd = new Random (0);
		for (int n: new Integer[] {4, 7, 9, 15, 16, 17, 23, 24, 25, 31, 32, 33, 63, 64, 65}) {
			for (int it=0; it < 10; it++) {
				PBSolution solution1 = generateRandomSolution (n);
				PBSolution solution2 = generateRandomSolution (n);
				
				int distance = solution1.hammingDistance(solution2);
				int computedDistance = 0;
				for (int i=0; i < n; i++) {
					if (solution1.getBit(i)!=solution2.getBit(i)) {
						computedDistance++;
					}
				}
				assertEquals(computedDistance, distance, "Error in Hamming distance computation");
			}
		}
	}
	
	private PBSolution generateRandomSolution(int n) {
		PBSolution solution = new PBSolution(n);
		for (int i=0; i < n; i++) {
			solution.setBit(i, rnd.nextInt(2));
		}
		return solution;
	}

}
