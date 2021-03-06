package neo.landscape.theory.apps.pseudoboolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

public class PBSolutionTest {

	@Test
	public void testPBSolution() {

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = new PBSolution(n);
			for (int i = 0; i < n; i++) {
				assertEquals("Not all bits set to zer at the beginning", 0,
						pbs.getBit(i));
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

				assertEquals("setBit of getBit not working", v, pbs.getBit(i));
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
				assertEquals("flipBit not working", 1, pbs.getBit(i));
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

				assertEquals("flipBit not working", 1 - v, pbs.getBit(i));

			}
		}

	}

	@Test
	public void testParse() {
		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = generateRandomSolution(rnd, n);

			PBSolution pos = new PBSolution(n);
			pos.parseBigEndian(pbs.toString());

			for (int i = 0; i < n; i++) {
				assertEquals("Error in parse or toString", pos.getBit(i),
						pbs.getBit(i));
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
			assertEquals("equals not working", pbs, pbs2);
		}
	}

	@Test
	public void testEquals2() {
		Random rnd = new Random(0);

		for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000 }) {
			PBSolution pbs = generateRandomSolution(rnd, n);
			PBSolution pbs2 = new PBSolution(pbs);
			assertEquals("equals not working", pbs, pbs2);
		}
	}
	
	@Test
	public void testHamming() {
	    Random rnd = new Random(0);

        for (int n : new int[] { 1, 5, 31, 32, 63, 64, 2000, 10000 }) {
            PBSolution pbs = generateRandomSolution(rnd, n);
            PBSolution pbs2 = generateRandomSolution(rnd, n);
            
            int distance = pbs.hammingDistance(pbs2);
            int distance2 = computeHammingDistanceByEnumeration(pbs, pbs2);
            
            assertEquals("hamming distance not working", distance2, distance);
        }
	}
	
	@Test
	public void testBase64Encoding() {
	    // TODO
	}

    private int computeHammingDistanceByEnumeration(PBSolution pbs, PBSolution pbs2) {
        int dis=0;
        for (int i=0; i < pbs.getN(); i++) {
            if (pbs.getBit(i) != pbs2.getBit(i)) {
                dis++;
            }
        }
        return dis;
    }

    private PBSolution generateRandomSolution(Random rnd, int n) {
        PBSolution pbs = new PBSolution(n);
        for (int i = 0; i < n; i++) {
            int v = rnd.nextInt(2);
            pbs.setBit(i, v);
        }
        return pbs;
    }

}
