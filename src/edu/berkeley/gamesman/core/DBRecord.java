package edu.berkeley.gamesman.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Collection;

public interface DBRecord {

	public byte byteValue();
	public DBRecord wrapValue(byte b);
	
	public void write(DataOutput out);
	public DBRecord wrap(DataInput in);
	
	public DBRecord fold(Collection<DBRecord> v);
	
}
