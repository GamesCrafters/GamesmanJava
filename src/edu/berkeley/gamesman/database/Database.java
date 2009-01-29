package edu.berkeley.gamesman.database;

public abstract class Database {

	public abstract void initialize(String url, Class<? extends DBValue> valueClass);
	
	public abstract DBValue getValue(Number loc);
	public abstract void setValue(Number loc, DBValue value);
	
	public abstract void flush();
	public abstract void close();
	
}
