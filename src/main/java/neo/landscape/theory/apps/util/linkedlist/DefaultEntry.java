package neo.landscape.theory.apps.util.linkedlist;

public class DefaultEntry<T> implements Entry<T> {

    private Entry<T> prev, next;
    private T v;
    
	public DefaultEntry(T v) {
		this.setV(v);
	}

	/* (non-Javadoc)
     * @see neo.landscape.theory.apps.util.Entry#getPrev()
     */
	@Override
    public Entry<T> getPrev() {
        return prev;
    }
    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.util.Entry#setPrev(neo.landscape.theory.apps.util.Entry)
     */
    @Override
    public void setPrev(Entry<T> prev) {
        this.prev = prev;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.util.Entry#getNext()
     */
    @Override
    public Entry<T> getNext() {
        return next;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.util.Entry#setNext(neo.landscape.theory.apps.util.Entry)
     */
    @Override
    public void setNext(Entry<T> next) {
        this.next = next;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.util.Entry#getV()
     */
    @Override
    public T getV() {
        return v;
    }

    /* (non-Javadoc)
     * @see neo.landscape.theory.apps.util.Entry#setV(T)
     */
    @Override
    public void setV(T v) {
        this.v = v;
    }

    
}