package edu.berkeley.gamesman.util;

import java.util.Iterator;

public class IteratorWrapper<T> implements Iterable<T> {
	Iterator<T> iter;
	public IteratorWrapper(Iterator<T> iter){
		this.iter = iter;
	}
	
	public Iterator<T> iterator() {
		return iter;
	}
}
