package neo.landscape.theory.apps.util.linkedlist;

public interface EntryFactory<T> {
    public Entry<T> getEntry(T t);
}
