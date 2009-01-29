package edu.berkeley.gamesman.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Collection;

public interface Record {

	public byte byteValue();
	public Record wrapValue(byte b);
	
	public void write(DataOutput out);
	public Record wrap(DataInput in);
	
	public Record fold(Collection<Record> v);
	
}
