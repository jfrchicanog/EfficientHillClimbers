package neo.landscape.theory.apps.util;

public class CpuClockTimer implements Timer {
	
    private long initTime;
    private boolean started=false;
    private long stopTime;
    private boolean stopSet=false;
    
    protected CpuClockTimer() {
    }
    
    @Override
	public void startTimer() {
        initTime = System.nanoTime();
        started=true;
    }

    @Override
	public long elapsedTime() {
        if (!started) {
            throw new RuntimeException("Timer not started");
        }
        return System.nanoTime()-initTime;
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
