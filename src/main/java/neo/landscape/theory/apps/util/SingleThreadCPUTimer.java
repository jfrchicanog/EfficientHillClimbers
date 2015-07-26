package neo.landscape.theory.apps.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class SingleThreadCPUTimer {
    
    private ThreadMXBean threadMXBean;
    private long initTime;
    private long threadID;
    private boolean started=false;
    private long stopTime;
    private boolean stopSet=false;
    
    public SingleThreadCPUTimer() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        threadID = Thread.currentThread().getId();
    }
    
    public void startTimer() {
        checkThread();
        initTime = threadMXBean.getCurrentThreadCpuTime();
        started=true;
    }

    private void checkThread() {
        if (threadID != Thread.currentThread().getId()) {
            throw new RuntimeException("Incorrect thread invoking the method");
        }
    }
    
    public long elapsedTime() {
        if (!started) {
            throw new RuntimeException("Timer not started");
        }
        checkThread();
        return threadMXBean.getCurrentThreadCpuTime()-initTime;
    }
    
    public void setStopTimeInNanoseconds(long stopNanoseconds) {
        stopTime = initTime + stopNanoseconds;
        stopSet = true;
    }
    
    public void setStopTimeMilliseconds(long stopMilliseconds) {
        setStopTimeInNanoseconds(stopMilliseconds*1000000);
    }
    
    public boolean shouldStop() {
        if (!stopSet) {
            throw new RuntimeException("Stop time not set");
        }
        return elapsedTime() >= stopTime;
    }
    
    

}
