package neo.landscape.theory.apps.graphcoloring;

import java.util.Arrays;

import neo.landscape.theory.apps.efficienthc.Solution;

public class WGCSolution implements Solution<WeightedGraphColoring> {

	public int[] colors;

	@Override
	public String toString() {
		return "WGCSolution [colors=" + Arrays.toString(colors) + "]";
	}

}
