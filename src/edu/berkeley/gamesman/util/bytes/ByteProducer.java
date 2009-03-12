package edu.berkeley.gamesman.util.bytes;

import org.apache.hadoop.fs.FSDataInputStream;

public interface ByteProducer {

	public byte get(int index);
	public long getLong(int idx);
	
}
