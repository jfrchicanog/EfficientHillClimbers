package neo.landscape.theory.apps.pseudoboolean.util.graphs;

public interface VerticesWithNMarks {
	int getLastNonEmptyBucket();
	boolean isBucketEmtpy(int bucketID);
	void clear();
	void addVertexToLastNonEmptyBucketOrZero(int vertex);
	void moveVertexToNextBucket(int bucket, int vertex);
	int removeVertexFromLastNonEmptyBucket();
}
