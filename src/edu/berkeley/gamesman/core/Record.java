package edu.berkeley.gamesman.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Collection;

public class Record {
	public void write(DataOutput out){
		
	}
	
	public static Record wrap(Configuration conf, DataInput in){
		return null;
	}
	
	public static int length(Configuration conf){
		return 1;
	}
	
}
