package neo.landscape.theory.apps.util;

public class Timers {
	public static final String SINGLE_THREAD_CPU="singleThreadCpu";
	public static final String CPU_CLOCK="cpuClock";
	
	
	public static Timer getSingleThreadCPUTimer() {
		return new SingleThreadCPUTimer();
	}
	
	public static String getNameOfDefaultTimer() {
		return SINGLE_THREAD_CPU;
	}
	
	public static Timer getDefaultTimer() {
		return getTimer(getNameOfDefaultTimer());
	}
	
	public static Timer getCpuClockTimer() {
		// TODO
		return null;
	}
	
	public static Timer getTimer(String name) {
		switch (name) {
		case SINGLE_THREAD_CPU:
			return getSingleThreadCPUTimer();
		case CPU_CLOCK:
			return getCpuClockTimer();
		default:
			throw new IllegalArgumentException("Timer "+name+" not recognized");
		}
	}

}
