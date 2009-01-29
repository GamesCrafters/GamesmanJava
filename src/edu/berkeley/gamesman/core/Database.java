package edu.berkeley.gamesman.core;

import java.math.BigInteger;


public abstract class Database {

	public abstract void initialize(String url, Configuration config, Record exampleValue);
	
	public abstract Record getValue(BigInteger loc);
	public abstract void setValue(BigInteger loc, Record value);
	
	public abstract void flush();
	public abstract void close();
	
}
