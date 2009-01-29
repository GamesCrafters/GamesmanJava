package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.database.DBRecord;

public class DBValueWritable implements Writable {

	private DBRecord value;
	
	public void readFields(DataInput in) throws IOException {
		value = value.wrap(in);
	}

	public void write(DataOutput out) throws IOException {
		value.write(out);
	}
	
	public DBRecord get(){
		return value;
	}
	
	public void set(DBRecord v){
		value = v;
	}

}
