package neo.landscape.theory.apps.pseudoboolean.util;

public final class Sample implements Cloneable{
	public long time;
	public double quality;
	
	public Sample (long t, double q)
	{
		time = t;
		quality = q;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return "Sample [time=" + time + ", quality=" + quality + "]";
	}
}