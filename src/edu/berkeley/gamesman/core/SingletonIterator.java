package edu.berkeley.gamesman.core;

import java.util.Iterator;

public final class SingletonIterator<E> implements Iterator<E> {

	E object;
	boolean done;
	
	/**
	 * An iterator that iterates over a single item
	 * @param item The item in this iterator
	 */
	public SingletonIterator(E item){
		object = item;
		done = false;
	}
	
	public boolean hasNext() {
		return !done;
	}

	public E next() {
		if(done)
			return null;
		done = true;
		return object;
	}

	public void remove() {
		done = true;
	}

}
