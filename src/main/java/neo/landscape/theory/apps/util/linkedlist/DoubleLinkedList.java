package neo.landscape.theory.apps.util.linkedlist;

import java.util.Iterator;

/**
 * Auxiliary double linked list in which we can access the entries.
 * 
 * @author francis
 *
 * @param <T>
 */
public class DoubleLinkedList<T> implements Iterable<T> {

	private Entry<T> first;
	private Entry<T> last;
	private EntryFactory<T> factory;

	public DoubleLinkedList() {
		this(new DefaultEntryFactory<T>());
	}
	
	public DoubleLinkedList(EntryFactory<T> factory) {
	    this.factory = factory;
        first = null;
        last = null;
    }

	public void remove(Entry<T> e) {
		if (e.getPrev() != null) {
			e.getPrev().setNext(e.getNext());
		} else {
			first = e.getNext();
		}

		if (e.getNext() != null) {
			e.getNext().setPrev(e.getPrev());
		} else {
			last = e.getPrev();
		}
	}

	public void add(Entry<T> e) {
		e.setPrev(null);
		e.setNext(first);
		if (first != null) {
			first.setPrev(e);
		}
		first = e;

		if (last == null) {
			last = first;
		}
	}

	/**
	 * Added to implement Adele's suggestion
	 * 
	 * @param e
	 */
	public void append(Entry<T> e) {
		e.setNext(null);
		e.setPrev(last);
		if (last != null) {
			last.setNext(e);
		}
		last = e;

		if (first == null) {
			first = last;
		}
	}

	public void add(T t) {
		add(factory.getEntry(t));
	}

	public Entry<T> getFirst() {
		return first;
	}

	public Entry<T> getLast() {
		return last;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private Entry<T> current = first;
			private Entry<T> last = null;

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public T next() {
				last = current;
				current = current.getNext();
				return last.getV();
			}

			@Override
			public void remove() {
				DoubleLinkedList.this.remove(last);
			}

		};
	}

	public boolean isEmpty() {
		return first == null;
	}

	public String toString() {
		String str = "[";

		Entry<T> e = first;
		while (e != null) {
			str += e.getV().toString() + ", ";
			e = e.getNext();
		}
		return str + "]";
	}

}
