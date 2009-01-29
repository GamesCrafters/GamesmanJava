package edu.berkeley.gamesman.database;

public abstract class Database<V> {

	public abstract void initialize(String url);
	
	public abstract V getValue(Number loc);
	public abstract void setValue(Number loc, V value);
	
	public abstract void flush();
	public abstract void close();
	
}
