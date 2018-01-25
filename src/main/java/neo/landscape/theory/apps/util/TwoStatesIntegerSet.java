package neo.landscape.theory.apps.util;

/**
 * 
 * @author Francisco Chicano
 * @email chicano@lcc.uma.es
 *
 *        This class represents a set of numbers with two states: explored and
 *        unexplored
 */

public interface TwoStatesIntegerSet {
	public void reset(); // All unexplored
	public int getNextUnexplored();
	public boolean hasMoreUnexplored();
	public void explored(int v);
	public void unexplored(int v);
	public boolean isExplored(int v);
	public int getNumberOfElements();
	public int getNumberOfExploredElements();

}
