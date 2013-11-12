package neo.landscape.theory.apps.util;

import java.util.Iterator;


/**
 * Auxiliary double linked list in which we can access the entries.
 * @author francis
 *
 * @param <T>
 */
public class DoubleLinkedList<T> implements Iterable<T>{
	
	public static class Entry<T> {
		
		public Entry(T v)
		{
			this.v=v;
		}
		
		private Entry<T> prev, next;
		public T v;
	}
	
	private Entry<T> first;
	private Entry<T> last;
	
	public DoubleLinkedList()
	{
		first =null;
		last = null;
	}
	
	public void remove(Entry<T> e)
	{
		if (e.prev !=null)
		{
			e.prev.next = e.next;
		}
		else
		{
			first = e.next;
		}
		
		if (e.next != null)
		{
			e.next.prev = e.prev;
		}
		else
		{
			last = e.prev;
		}
	}
	
	public void add(Entry<T> e)
	{
		e.prev=null;
		e.next = first;
		if (first != null)
		{
			first.prev = e;
		}
		first = e;
		
		if (last==null)
		{
			last=first;
		}
	}
	
	/**
	 * Added to implement Adele's suggestion
	 * @param e
	 */
	public void append(Entry<T> e)
	{
		e.next=null;
		e.prev = last;
		if (last != null)
		{
			last.next = e;
		}
		last = e;
		
		if (first==null)
		{
			first=last;
		}
	}
	
	public void add (T t)
	{
		add (new Entry(t));
	}
	
	public Entry<T> getFirst()
	{
		return first;
	}
	
	public Entry<T> getLast()
	{
		return last;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>(){
			private Entry<T> current=first;
			private Entry<T> last=null;
			@Override
			public boolean hasNext() {
				return current!=null;
			}

			@Override
			public T next() {
				last = current;
				current = current.next;
				return last.v;
			}

			@Override
			public void remove() {
				DoubleLinkedList.this.remove(last);
			}
			
		};
	}
	
	public boolean isEmpty()
	{
		return first ==null;
	}
	
	public String toString()
	{
		String str = "[";
		
		Entry<T> e = first;
		while (e != null)
		{
			str += e.v.toString() +", ";
			e = e.next;
		}
		
		return str+"]";
	}

}
