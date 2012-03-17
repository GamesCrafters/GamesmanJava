package edu.berkeley.gamesman.propogater.factory;

public interface Factory<T> {
	public T create();
	
	public void reset(T obj);
}
