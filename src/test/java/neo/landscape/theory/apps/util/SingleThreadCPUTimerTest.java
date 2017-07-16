package neo.landscape.theory.apps.util;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleThreadCPUTimerTest {

    private SingleThreadCPUTimer cpuTimer;
    
    @Before
    public void setup() {
        cpuTimer = new SingleThreadCPUTimer();
    }
    
    @Test(expected=RuntimeException.class)
    public void testElapsedTimeException() {
        cpuTimer.elapsedTime();
    }
    
    @Test
    public void testShouldStopException() {
        Assert.assertFalse(cpuTimer.shouldStop());
    }
    
    @Test
    public void testShouldStopNotSetException() {
        cpuTimer.startTimer();
        Assert.assertFalse(cpuTimer.shouldStop());
    }

}
