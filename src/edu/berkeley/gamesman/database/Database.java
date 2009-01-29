package edu.berkeley.gamesman.database;

public interface Database<V> {

	public void initialize(String url);
	
	public V getValue(Number loc);
	public void setValue(Number loc, V value);
	
	public void flush();
	public void close();
	
}
