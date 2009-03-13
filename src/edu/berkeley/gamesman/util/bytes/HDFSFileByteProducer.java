package edu.berkeley.gamesman.util.bytes;

import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.util.Util;

public class HDFSFileByteProducer implements ByteProducer {
	
	final FSDataInputStream din;
	final long len;
	final Path path;

	public HDFSFileByteProducer(FSDataInputStream din, Path p, long len) {
		this.din = din;
		this.len = len;
		this.path = p;
	}

	public synchronized byte get(int index) {
		if(index >= len) return 0;
		try {
			byte b;
			din.seek(index);
			b = din.readByte();
			System.out.println("byteread "+index+" => "+b);
			return b;
		} catch (IOException e) {
			Util.fatalError("Could not read from HDFS "+path+" @"+index,e);
		}
		assert false;
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
