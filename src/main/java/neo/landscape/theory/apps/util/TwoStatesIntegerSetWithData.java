package neo.landscape.theory.apps.util;

import java.util.stream.IntStream;

/**
 * 
 * @author Francisco Chicano
 * @email chicano@lcc.uma.es
 *
 *        This class represents a set of numbers with two states: explored and
 *        unexplored
 */

public interface TwoStatesIntegerSetWithData<T> extends TwoStatesIntegerSet{
    public interface DataFactory<T> {
        public T newData();
        public void recycle(T data);
    }
	public T getData(int v);
	public void setData(int v, T data);
	public void setDataFactory(DataFactory<T> factory);

}
