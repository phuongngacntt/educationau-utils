package au.edu.educationau.opensource.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * A set which will never grow in size beyond a maxSize paramter passed to the constructor. 
 * 
 * This class simply wraps another SortedSet, and the last element will be removed anytime a new 
 * element is added which would grow the set beyond maxSize. By creating the wrapped SortedSet using
 * a custom Comparator this behaviour can be customized somewhat. 
 * 
 * 
 * @author nlothian
 *
 * @param <E>
 */
public class MaxSizedSet<E> implements SortedSet<E> {
	private SortedSet<E> set;
	private int maxSize;
	
	
	public MaxSizedSet(SortedSet<E> set, int maxSize) {
		super();
		
		this.maxSize = maxSize;
		this.set = set;
	}


	public boolean add(E e) {
		if (set.size() >= maxSize) {
			set.remove(set.last());
		}
		return set.add(e);
	}


	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for (E entry : c) {
			boolean changed = this.add(entry);
			result = result || changed;
		}
		return result;
	}


	public void clear() {
		set.clear();
	}


	public Comparator<? super E> comparator() {
		return set.comparator();
	}


	public boolean contains(Object o) {
		return set.contains(o);
	}


	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		return set.equals(o);
	}


	public E first() {
		return set.first();
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}


	public SortedSet<E> headSet(E toElement) {
		return set.headSet(toElement);
	}


	public boolean isEmpty() {
		return set.isEmpty();
	}


	public Iterator<E> iterator() {
		return set.iterator();
	}


	public E last() {
		return set.last();
	}


	public boolean remove(Object o) {
		return set.remove(o);
	}


	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}


	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}


	public int size() {
		return set.size();
	}


	public SortedSet<E> subSet(E fromElement, E toElement) {
		return set.subSet(fromElement, toElement);
	}


	public SortedSet<E> tailSet(E fromElement) {
		return set.tailSet(fromElement);
	}


	public Object[] toArray() {
		return set.toArray();
	}


	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}


	@Override
	public String toString() {		
		return super.toString() + " maxSize = " + maxSize + " contains [ " + set.toString() + " ]";
	}




}
