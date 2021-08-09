package neo.landscape.theory.apps.pseudoboolean.px;

import java.util.function.Function;

public class IndexAssigner implements Function<Long,Long> {
	private long index=0;
	@Override
	public Long apply(Long arraySize) {
		long thisIndex = index;
		index += arraySize;
		return thisIndex;
	}
	public long getIndex() {
		return index;
	}
	
	public void clearIndex() {
		index=0;
	}
}