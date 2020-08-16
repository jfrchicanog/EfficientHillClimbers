package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.function.Function;

public class IndexAssigner implements Function<Integer,Integer> {
	private int index=0;
	@Override
	public Integer apply(Integer arraySize) {
		int thisIndex = index;
		index += arraySize;
		return thisIndex;
	}
	public int getIndex() {
		return index;
	}
	
	public void clearIndex() {
		index=0;
	}
}