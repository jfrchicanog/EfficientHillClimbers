package neo.landscape.theory.apps.util;

public interface Adaptor<S, T> {
    public S adapt(T object);
}
