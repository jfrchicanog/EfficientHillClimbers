package neo.landscape.theory.apps.pseudoboolean.util;


public class AveragedSample {
    private long minTime;
    private double error;
    private int samples;
    private int solved;
    
    public void setError(double error) {
        this.error = error;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

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
        this.solved=0;
        for (Sample tmp : past) {
        	if (tmp != null) {
        		this.samples = samples + 1;
        		this.error = error + tmp.quality;
        		if (tmp.quality == 0.0) {
        		    this.solved++;
        		}
        	}
        }
        this.error = error / samples;
    }

    public String toString() {
        return getMinTime() + ", " + getError() + ", " + getSamples() + ", " + getSolved();
    }

    public int getSolved() {
        return solved;
    }

}