package neo.landscape.theory.apps.pseudoboolean.util;


public class AveragedSample {
    private long minTime;
    private double error;
    public void setError(double error) {
        this.error = error;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    private int samples;

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long min_time) {
        this.minTime = min_time;
    }

    public double getError() {
        return error;
    }

    public int getSamples() {
        return samples;
    }

    public AveragedSample() {
    }

    public void computeStatisticsForSelectedSamples(Sample[] past) {
        this.error = (double) 0;
        this.samples = 0;
        for (Sample tmp : past) {
        	if (tmp != null) {
        		this.samples = samples + 1;
        		this.error = error + tmp.quality;
        	}
        }
        this.error = error / samples;
    }

    public String toString() {
        return getMinTime() + ", " + getError() + ", " + getSamples();
    }
}