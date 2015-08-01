package neo.landscape.theory.apps.util;

import static org.junit.Assert.fail;

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
    
    @Test(expected=RuntimeException.class)
    public void testShouldStopException() {
        cpuTimer.shouldStop();
    }
    
    @Test(expected=RuntimeException.class)
    public void testShouldStopNotSetException() {
        cpuTimer.startTimer();
        cpuTimer.shouldStop();
    }

}
