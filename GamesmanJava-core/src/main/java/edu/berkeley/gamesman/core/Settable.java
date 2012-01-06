package edu.berkeley.gamesman.core;

public interface Settable<T extends Settable<T>> {
	public void set(T t);
}
