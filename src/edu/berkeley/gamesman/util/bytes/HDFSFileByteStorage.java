package edu.berkeley.gamesman.util.bytes;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;

import edu.berkeley.gamesman.util.Util;

public final class HDFSFileByteStorage implements ByteStorage {

	final FSDataOutputStream dout;
	final byte[] back;
	
	public HDFSFileByteStorage(FSDataOutputStream dout, int size) {
		this.dout = dout;
		back = new byte[size];
	}

	public void put(int idx, byte b) {
		//try {
			back[idx] = b;
		//	if(idx != dout.getPos())
		//		Util.fatalError("HDFS files must be written sequentially!");
		//	dout.write(b);
		//} catch (IOException e) {
		//	Util.fatalError("Could not write to HDFS",e);
		//}
	}
	
	private void put(int idx, long l){
		put(idx,(byte)l);
	}

	public void putLong(int idx, long l) {
		for(int i = 0; i <= 60; i += 4)
			put(idx+i,l >> 60-i);
	}

	public synchronized byte get(int index) {
		/*try {
			din.seek(index);
			return din.readByte();
		} catch (IOException e) {
			Util.fatalError("Could not read from HDFS",e);
		}
		return 0; // NOT REACHED*/
		return back[index];
	}

	public synchronized long getLong(int idx) {
		long accum = 0;
		for(int i = 0; i <= 60; i += 4)
			accum |= get(idx+i) << 60-i;
		return accum;
	}

	public void close() {
		try{
			dout.write(back);
			dout.close();
		}catch(IOException e){
			Util.fatalError("Could not write out file",e);
		}
	}

}
