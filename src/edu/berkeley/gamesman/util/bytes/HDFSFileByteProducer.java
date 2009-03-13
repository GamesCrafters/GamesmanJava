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
			//System.out.println("byteread "+index+" => "+b);
			return b;
		} catch (IOException e) {
			Util.fatalError("Could not read from HDFS "+path+" @"+index,e);
		}
		assert false;
		return 0; // NOT REACHED
	}

	public synchronized long getLong(int index) {
		if(index >= len) return 0;
		try {
			long b;
			din.seek(index);
			b = din.readLong();
			//System.out.println("longread "+index+" => "+b);
			return b;
		} catch (IOException e) {
			Util.fatalError("Could not read from HDFS "+path+" @"+index,e);
		}
		assert false;
		return 0; // NOT REACHED
	}

}
