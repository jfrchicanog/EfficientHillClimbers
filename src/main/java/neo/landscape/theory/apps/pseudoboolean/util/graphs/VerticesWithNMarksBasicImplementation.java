package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class VerticesWithNMarksBasicImplementation implements VerticesWIthNMarks {
	private List<Integer> [] data;
	private int lastNonEmpty;
	
	public VerticesWithNMarksBasicImplementation(int maxBuckets) {
		data = new List [maxBuckets];
		for (int i=0; i < data.length; i++) {
			data[i] = new ArrayList<>();
		}
		lastNonEmpty = -1;
	}
	
	@Override
	public int getLastNonEmptyBucket() {
		return lastNonEmpty;
	}

	@Override
	public boolean isBucketEmtpy(int bucketID) {
		if (bucketID < 0 || bucketID >= data.length) {
			throw new IllegalArgumentException();
		}
		return data[bucketID].isEmpty();
	}

	@Override
	public void clear() {
		for (int i=0; i < data.length; i++) {
			data[i].clear();
		}
		lastNonEmpty = -1;
	}

	@Override
	public void addVertexToLastNonEmptyBucketOrZero(int vertex) {
		if (lastNonEmpty >= 0) {
			data[lastNonEmpty].add(vertex);
		} else {
			data[0].add(vertex);
			lastNonEmpty=0;
		}
	}

	@Override
	public void moveVertexToNextBucket(int bucket, int vertex) {
		if (bucket == data.length-1) {
			throw new RuntimeException("There are no more buckets");
		}
		int index = data[bucket].indexOf(vertex);
		data[bucket].remove(index);
		data[bucket+1].add(vertex);
		
		if (bucket+1 > lastNonEmpty) {
			lastNonEmpty = bucket+1;
		}
	}

	@Override
	public int removeVertexFromLastNonEmptyBucket() {
		if (lastNonEmpty < 0) {
			throw new NoSuchElementException();
		}
		
		int result = data[lastNonEmpty].remove(data[lastNonEmpty].size()-1);
		while (lastNonEmpty >= 0 && data[lastNonEmpty].isEmpty()) {
			lastNonEmpty--;
		}
		return result;
	}

}
