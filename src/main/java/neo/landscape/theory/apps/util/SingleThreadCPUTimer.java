package neo.landscape.theory.apps.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class SingleThreadCPUTimer implements Timer {
    
    private ThreadMXBean threadMXBean;
    private long initTime;
    private long threadID;
    private boolean started=false;
    private long stopTime;
    private boolean stopSet=false;
    
    protected SingleThreadCPUTimer() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        threadID = Thread.currentThread().getId();
    }
    
    @Override
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
    
    @Override
	public long elapsedTime() {
        if (!started) {
            throw new RuntimeException("Timer not started");
        }
        checkThread();
        return threadMXBean.getCurrentThreadCpuTime()-initTime;
    }
    
    @Override
	public void setStopTimeInNanoseconds(long stopNanoseconds) {
        stopTime = initTime + stopNanoseconds;
        stopSet = true;
    }
    
    @Override
	public void setStopTimeMilliseconds(long stopMilliseconds) {
        setStopTimeInNanoseconds(stopMilliseconds*1000000);
    }
    
    @Override
	public boolean shouldStop() {
        if (!stopSet) {
            return false;
        }
        return elapsedTime() >= stopTime;
    }
    
    @Override
	public long elapsedTimeInMilliseconds() {
        return elapsedTime()/1000000;
    }

    @Override
	public boolean isStopSet() {
        return stopSet;
    }

}
