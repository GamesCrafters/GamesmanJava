package edu.berkeley.gamesman.util;

import java.util.Iterator;

/**
 * A wrapper of an Iterator that is actually Iterable
 * @see "http://forums.java.net/jive/thread.jspa?threadID=318"
 * 
 * @author Steven Schlansker
 *
 * @param <T> Type that the iterator will iterate over
 */
public class IteratorWrapper<T> implements Iterable<T> {
	final Iterator<T> iter;
	/**
	 * Create a wrapper over an Iterator
	 * @param iter the iterator to wrap
	 */
	public IteratorWrapper(final Iterator<T> iter){
		this.iter = iter;
	}
	
	public Iterator<T> iterator() {
		return iter;
	}
}
