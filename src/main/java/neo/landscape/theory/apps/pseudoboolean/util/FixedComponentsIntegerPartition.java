package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.NoSuchElementException;
import java.util.Random;

import neo.landscape.theory.apps.util.IteratorFromArray;

public class FixedComponentsIntegerPartition {
    
    private int [] values;
    private int [] inverse;
    private int [] bucketIndices;
    private Random rnd;

    public FixedComponentsIntegerPartition(int n, int buckets, long seed) {
        this(n, buckets, seed, 0);
    }
    
    public FixedComponentsIntegerPartition(int n, int buckets, long seed, int defaultBucket) {
        bucketIndices = new int [buckets+1];
        values = new int[n];
        inverse = new int [n];
        
        for (int i=0; i < n; i++) {
            values[i]=i;
            inverse[i]=i;
            bucketIndices[defaultBucket+1]++;
        }
        
        for(int i=defaultBucket+2; i < bucketIndices.length; i++) {
            bucketIndices[i] = bucketIndices[i-1]; 
        }

        rnd = new Random(seed);
    }
        
    public Iterable<Integer> iterableOverSet() {
        return IteratorFromArray.iterable(values);
    }
    
    public Iterable<Integer> iterableOverBucket(int bucket) {
        return IteratorFromArray.iterable(values, bucketIndices[bucket], bucketIndices[bucket+1]);
    }

    
    public int getNumberOfBuckets() {
        return bucketIndices.length-1;
    }
    
    public int getNumberOfElements() {
        return values.length;
    }

    public int sizeOfBucket(int bucket) {
        return bucketIndices[bucket+1]-bucketIndices[bucket];
    }
    
    public boolean isBucketEmpty(int bucket) {
        return bucketIndices[bucket]==bucketIndices[bucket+1];
    }
     
    public int getRandomElement(int bucket) {
        return getRandomElement(bucket, bucket);
    }
        
    public int getRandomElement(int fromBucket, int toBucket) {
        if (bucketIndices[fromBucket]>=bucketIndices[toBucket+1]) {
            throw new NoSuchElementException();
        }
        int size = bucketIndices[toBucket+1]-bucketIndices[fromBucket];
        int randomIndex = bucketIndices[fromBucket] + rnd.nextInt(size);
        return values[randomIndex];
    }
    
    public void moveElement(int oldBucket, int newBucket, int value) {
        while (oldBucket < newBucket) {
            moveUpInBucketList(oldBucket, value);
            oldBucket++;
        }
        
        while (oldBucket > newBucket) {
            moveDownInBucketList(oldBucket, value);
            oldBucket--;
        }
    }
    
    public void joinDownConsecutiveBuckets(int fromBucket, int toBucket) {
        joinConsecutiveBuckets(fromBucket, toBucket, fromBucket);
    }
    
    public void joinConsecutiveBuckets(int fromBucket, int toBucket, int targetBucket) {
        if (fromBucket > toBucket) {
            throw new IllegalArgumentException("First bucket is higher than last bucket");
        }
        
        if (targetBucket < fromBucket || targetBucket > toBucket) {
            throw new IllegalArgumentException("The target bucket must be between the first and the last");
        }
        
        for (int bucket=fromBucket+1; bucket <= targetBucket; bucket++) {
            bucketIndices[bucket] = bucketIndices[fromBucket];
        }
        
        for (int bucket=targetBucket+1; bucket <= toBucket; bucket++) {
            bucketIndices[bucket] = bucketIndices[toBucket+1];
        }
    }
    
    public boolean isElementInBucket(int value, int bucket) {
        return (bucketIndices[bucket] <= inverse[value]) &&
                inverse[value] < bucketIndices[bucket+1];
    }
    
    

    private void moveUpInBucketList(int oldBucket, int value) {
        int target = bucketIndices[oldBucket+1]-1;
        swapValues(value, target);
        bucketIndices[oldBucket+1]--;
    }
    
    private void moveDownInBucketList(int oldBucket, int value) {
        int target = bucketIndices[oldBucket];
        swapValues(value, target);
        bucketIndices[oldBucket]++;
    }

    private void swapValues(int value, int target) {
        int source = inverse[value];
        if (source != target) {
            values[source] = values[target];
            values[target] = value;
            
            inverse[value] = target;
            inverse[values[source]] = source;
        }
    }
    

    
}
