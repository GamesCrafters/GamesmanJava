package edu.berkeley.gamesman.core;

import java.math.BigInteger;

import edu.berkeley.gamesman.database.DBRecord;

public abstract class Database {

	public abstract void initialize(String url, Configuration config, DBRecord exampleValue);
	
	public abstract DBRecord getValue(BigInteger loc);
	public abstract void setValue(BigInteger loc, DBRecord value);
	
	public abstract void flush();
	public abstract void close();
	
}
