package neo.landscape.theory.apps.util.linkedlist;


public interface Entry<T> {

    public abstract Entry<T> getPrev();
    public abstract void setPrev(Entry<T> prev);
    public abstract Entry<T> getNext();
    public abstract void setNext(Entry<T> next);
    public abstract T getV();

}