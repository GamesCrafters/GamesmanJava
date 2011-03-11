package edu.berkeley.gamesman.util.qll;

public interface Factory<T> {
	public T newObject();

	public void reset(T t);
}
