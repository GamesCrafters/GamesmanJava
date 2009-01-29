package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.database.DBValue;

public abstract class Database {

	public abstract void initialize(String url, Configuration config, DBValue exampleValue);
	
	public abstract DBValue getValue(Number loc);
	public abstract void setValue(Number loc, DBValue value);
	
	public abstract void flush();
	public abstract void close();
	
}
