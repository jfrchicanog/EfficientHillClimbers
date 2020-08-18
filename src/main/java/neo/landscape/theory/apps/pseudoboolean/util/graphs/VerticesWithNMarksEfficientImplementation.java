package neo.landscape.theory.apps.pseudoboolean.util.graphs;

import java.util.NoSuchElementException;

public class VerticesWithNMarksEfficientImplementation implements VerticesWIthNMarks {
	private int [] vertexIndex;
	private int [] vertices;
	private int [] bucketIndex;
	
	private int numberOfBuckets;
	
	public VerticesWithNMarksEfficientImplementation(int elements, int maxBuckets) {
		if (maxBuckets <= 0) {
			throw new IllegalArgumentException();
		}
		
		vertexIndex = new int [elements];
		vertices = new int [elements];
		bucketIndex = new int [maxBuckets];
		clear();
	}
	
	@Override
	public int getLastNonEmptyBucket() {
		return numberOfBuckets-1;
	}

	@Override
	public boolean isBucketEmtpy(int bucketID) {
		if (bucketID < 0 || bucketID >= bucketIndex.length) {
			throw new IllegalArgumentException();
		}
		if (bucketID >= numberOfBuckets) {
			return true;
		}
		
		if (bucketID == 0) {
			return bucketIndex[0] < 0;
		}
		return bucketIndex[bucketID] <= bucketIndex[bucketID-1];
	}

	@Override
	public void clear() {
		numberOfBuckets = 0;
	}

	@Override
	public void addVertexToLastNonEmptyBucketOrZero(int vertex) {
		if (vertex >= vertexIndex.length) {
			throw new NoSuchElementException();
		}
		
		if (numberOfBuckets == 0) {
			numberOfBuckets=1;
			bucketIndex[0]=-1;
		}

		int index = ++bucketIndex[numberOfBuckets-1];
		vertices[index] = vertex;
		vertexIndex[vertex] = index;
	}

	@Override
	public void moveVertexToNextBucket(int bucket, int vertex) {
		if (bucket >= numberOfBuckets || vertex >= vertexIndex.length) {
			throw new NoSuchElementException("Incorrect bucket or vertex");
		}
		
		if (bucket >= bucketIndex.length-1) {
			throw new NoSuchElementException("No more buckets");
		}

		int index = vertexIndex[vertex];
		
		assert index >= 0;
		assert index <= bucketIndex[bucket];
		assert bucket == 0 || bucketIndex[bucket-1] < index;
		assert vertices[index] == vertex;
		
		if (index != bucketIndex[bucket]) {
			int tmp = vertices[bucketIndex[bucket]];
			vertices[index] = tmp;
			vertexIndex[tmp] = index;
			vertices[bucketIndex[bucket]] = vertex;
			vertexIndex[vertex] = bucketIndex[bucket];
		}
		
		if (bucket == numberOfBuckets-1) {
			bucketIndex[numberOfBuckets++] = bucketIndex[bucket];
		}
		
		bucketIndex[bucket]--;
	}

	@Override
	public int removeVertexFromLastNonEmptyBucket() {
		if (numberOfBuckets==0) {
			throw new NoSuchElementException();	
		}
		
		int result = vertices[bucketIndex[numberOfBuckets-1]--];
		
		while (numberOfBuckets > 0 && isBucketEmtpy(numberOfBuckets-1)) {
			numberOfBuckets--;
		}
		
		return result;
	}

}
