package neo.landscape.theory.apps.graphcoloring;

import neo.landscape.theory.apps.efficienthc.Move;

public class WGCMove implements Move<WeightedGraphColoring, WGCSolution> {

	public int color;
	public int vertex;
	public double improvement;

	public WGCMove(int v, int c) {
		vertex = v;
		color = c;
	}

	public WGCMove(int v, int c, double i) {
		color = c;
		vertex = v;
		improvement = i;
	}

	@Override
	public double getImprovement() {
		return improvement;
	}

}
