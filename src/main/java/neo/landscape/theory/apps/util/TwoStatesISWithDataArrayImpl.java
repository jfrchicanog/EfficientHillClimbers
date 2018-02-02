package neo.landscape.theory.apps.util;


public class TwoStatesISWithDataArrayImpl<T> extends TwoStatesISArrayImpl implements TwoStatesIntegerSetWithData<T>{
    protected T [] data;
    protected DataFactory<T> factory;
    
    public TwoStatesISWithDataArrayImpl(int n) {
        super(n);
        data = (T [])new Object[n];
    }

    @Override
    public T getData(int v) {
        if (data[v]==null && factory != null) {
            data[v] = factory.newData();
        }
        return data[v];
    }

    @Override
    public void setData(int v, T data) {
        this.data[v] = data;
        
    }

    @Override
    public void setDataFactory(DataFactory<T> factory) {
        this.factory = factory;
    }
}
