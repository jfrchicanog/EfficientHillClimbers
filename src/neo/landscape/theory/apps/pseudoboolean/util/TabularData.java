package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Map;

public final class TabularData {
	public Map<String,Sample[]> results;
	public String [] algorithms;
	
	public TabularData(Map<String,Sample[]> r, String [] a)
	{
		results = r;
		algorithms= a;
	}
}