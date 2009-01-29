package edu.berkeley.gamesman.database;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Collection;

public interface DBValue {

	public byte byteValue();
	public DBValue wrapValue(byte b);
	
	public void write(DataOutput out);
	public DBValue wrap(DataInput in);
	
	public DBValue fold(Collection<DBValue> v);
	
}
