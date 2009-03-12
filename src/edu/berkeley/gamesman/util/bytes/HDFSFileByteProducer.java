package edu.berkeley.gamesman.util.bytes;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;

import edu.berkeley.gamesman.util.Util;

public class HDFSFileByteProducer implements ByteProducer {
	
	final FSDataInputStream din;

	public HDFSFileByteProducer(FSDataInputStream din) {
		this.din = din;
	}

	public synchronized byte get(int index) {
		try {
			din.seek(index);
			return din.readByte();
		} catch (IOException e) {
			Util.fatalError("Could not read from HDFS",e);
		}
		return 0; // NOT REACHED
	}

	public synchronized long getLong(int idx) {
		return 
		get(idx)    << 60 | get(idx+4)  << 56 | get(idx+8)  << 52 | get(idx+12) << 48 |
		get(idx+16) << 44 | get(idx+20) << 40 | get(idx+24) << 36 | get(idx+28) << 32 |
		get(idx+32) << 28 | get(idx+36) << 24 | get(idx+40) << 20 | get(idx+44) << 16 |
		get(idx+48) << 12 | get(idx+52) << 8  | get(idx+56) << 4  | get(idx+60);
	}

}
