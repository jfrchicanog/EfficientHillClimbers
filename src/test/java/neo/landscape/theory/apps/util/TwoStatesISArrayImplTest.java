package neo.landscape.theory.apps.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class TwoStatesISArrayImplTest {

	private TwoStatesIntegerSet set;

	@Before
	public void setup() {
		set = new TwoStatesISArrayImpl(10);
	}

	@Test
	public void testAllUnexplored() {
		int n = set.getNumberOfElements();
		for (int i = 0; i < n; i++) {
			assertFalse("Unepected explored element", set.isExplored(i));
		}
	}

	@Test
	public void testRandom() {
		Random rnd = new Random(0);
		List<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < set.getNumberOfElements(); i++) {
			values.add(i);
		}

		for (int i = 0; i < 10; i++) {
			set.reset();

			Collections.shuffle(values, rnd);
			int els = rnd.nextInt(values.size());

			List<Integer> exp = values.subList(0, els + 1);

			exploreSequence(set, exp);
			checkExplored(set, exp);
			checkUnexplored(set, values.subList(els + 1, values.size()));

		}

	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgumentException1() {
		set.explored(set.getNumberOfElements() + 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgumentException2() {
		set.unexplored(set.getNumberOfElements() + 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgumentException3() {
		set.isExplored(set.getNumberOfElements() + 1);
	}

	@Test
	public void testSimilarBehaviour() {
		TwoStatesIntegerSet another = new TwoStatesISSetImpl(
				set.getNumberOfElements());

		Random rnd = new Random(0);
		List<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < set.getNumberOfElements(); i++) {
			values.add(i);
		}

		for (int i = 0; i < 10; i++) {
			set.reset();
			another.reset();

			Collections.shuffle(values, rnd);
			int els = rnd.nextInt(values.size());

			List<Integer> exp = values.subList(0, els + 1);

			exploreSequence(set, exp);
			exploreSequence(another, exp);

			checkSameState(set, another);

		}

	}

	private void checkSameState(TwoStatesIntegerSet set1,
			TwoStatesIntegerSet set2) {
		assertEquals(set1.getNumberOfElements(), set2.getNumberOfElements());
		for (int i = 0; i < set1.getNumberOfElements(); i++) {
			assertEquals(set1.isExplored(i), set2.isExplored(i));
		}
	}

	private void exploreSequence(TwoStatesIntegerSet set, List<Integer> elements) {
		for (int i : elements) {
			set.explored(i);
		}
	}

	private void unexploredSequence(TwoStatesIntegerSet set,
			List<Integer> elements) {
		for (int i : elements) {
			set.unexplored(i);
		}
	}

	private void checkExplored(TwoStatesIntegerSet set, List<Integer> explored) {
		for (int i : explored) {
			assertTrue("Unexpected unexplored element", set.isExplored(i));
		}
	}

	private void checkUnexplored(TwoStatesIntegerSet set,
			List<Integer> unexplored) {
		for (int i : unexplored) {
			assertFalse("Unexpected explored element", set.isExplored(i));
		}
	}

}
