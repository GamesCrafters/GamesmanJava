package edu.berkeley.gamesman.database;

import java.util.Collection;

public interface DBValue {

	public byte byteValue();
	public DBValue wrapValue(byte b);
	public DBValue fold(Collection<DBValue> v);
	
}
