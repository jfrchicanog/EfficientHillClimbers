package neo.landscape.theory.apps.util.linkedlist;

public class DefaultEntryFactory<T> implements EntryFactory<T> {

    @Override
    public Entry<T> getEntry(T t) {
        return new DefaultEntry<T>(t);
    }

}
