package neo.landscape.theory.apps.pseudoboolean.util;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class FixedComponentsIntegerPartitionTest {
    
    private FixedComponentsIntegerPartition partition;

    @Before
    public void setUp() throws Exception {
        partition = new FixedComponentsIntegerPartition(100, 4, 5);
    }

    @Test
    public void testBasicAccesors() {
        Assert.assertEquals(100, partition.getNumberOfElements());
        Assert.assertEquals(4, partition.getNumberOfBuckets());
        int count =0;
        for (int val: partition.iterableOverSet()) {
            count++;
            Assert.assertTrue(partition.isElementInBucket(val, 0));
        }
        Assert.assertEquals(100, count);
    }
    
    @Test
    public void testMovement() {
        for (int val=0; val < 100; val++) {
            partition.moveElement(0, val%4, val);
            Assert.assertTrue(partition.isElementInBucket(val, val%4));
        }
        
    }
    
    @Test
    public void testJoin() {
        for (int val=0; val < 100; val++) {
            partition.moveElement(0, val%4, val);
        }
        
        int bucket0Size = partition.sizeOfBucket(0);
        int bucket1Size = partition.sizeOfBucket(1);
        
        partition.joinDownConsecutiveBuckets(0, 1);
        
        Assert.assertEquals(bucket0Size+bucket1Size, partition.sizeOfBucket(0));
        Assert.assertEquals(0, partition.sizeOfBucket(1));
        
        for (int val: partition.iterableOverBucket(0)) {
            Assert.assertTrue(((val%4)>>1)==0);
        }
        
        for (int bucket=2; bucket < 4; bucket++) {
            for (int val: partition.iterableOverBucket(bucket)) {
                Assert.assertTrue(val%4==bucket);
            }
        }
        
    }
    

}
