package neo.landscape.theory.apps.util;

public interface Timer {

	void startTimer();

	long elapsedTime();

	void setStopTimeInNanoseconds(long stopNanoseconds);

	void setStopTimeMilliseconds(long stopMilliseconds);

	boolean shouldStop();

	long elapsedTimeInMilliseconds();

	boolean isStopSet();

}