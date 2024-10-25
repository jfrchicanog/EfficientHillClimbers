package neo.landscape.theory.apps.util;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class TwoStatesISArrayImplTest {

	private TwoStatesIntegerSet set;

	@BeforeEach
	public void setup() {
		set = new TwoStatesISArrayImpl(10);
	}

	@Test
	public void testAllUnexplored() {
		int n = set.getNumberOfElements();
		for (int i = 0; i < n; i++) {
			assertFalse(set.isExplored(i), "Unepected explored element");
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

	@Test
	public void testIllegalArgumentException1() {
		assertThrows(IllegalArgumentException.class, () -> {
			set.explored(set.getNumberOfElements() + 1);
		});
	}

	@Test()
	public void testIllegalArgumentException2() {
		assertThrows(IllegalArgumentException.class, ()->{
			set.unexplored(set.getNumberOfElements() + 1);
		});
	}

	@Test
	public void testIllegalArgumentException3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			set.isExplored(set.getNumberOfElements() + 1);
		});
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
		assertThat(set1.getNumberOfElements()).isEqualTo(set2.getNumberOfElements());
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
			assertTrue(set.isExplored(i),"Unexpected unexplored element");
		}
	}

	private void checkUnexplored(TwoStatesIntegerSet set,
			List<Integer> unexplored) {
		for (int i : unexplored) {
			assertFalse(set.isExplored(i),"Unexpected explored element");
		}
	}

}
