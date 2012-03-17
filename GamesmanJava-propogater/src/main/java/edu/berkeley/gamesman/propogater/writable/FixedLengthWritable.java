package edu.berkeley.gamesman.propogater.writable;

import org.apache.hadoop.io.Writable;

public interface FixedLengthWritable extends Writable {
	public int size();
}
