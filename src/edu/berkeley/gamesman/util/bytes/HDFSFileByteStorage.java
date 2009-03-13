package edu.berkeley.gamesman.util.bytes;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;

import edu.berkeley.gamesman.util.Util;

public final class HDFSFileByteStorage implements ByteStorage {

	final FSDataOutputStream dout;
	final ArrayList<byte[]> back = new ArrayList<byte[]>();
	private long size;
	
	private final int SLICE_SIZE = 100*1024;
	
	public HDFSFileByteStorage(FSDataOutputStream dout) {
		this.dout = dout;
	}

	public void put(int idx, byte b) {
		if(size < idx){
			for(long s = size; s < idx+SLICE_SIZE; s += SLICE_SIZE)
				back.add(new byte[SLICE_SIZE]);
			size = SLICE_SIZE*back.size();
		}
		back.get(idx/SLICE_SIZE)[idx % SLICE_SIZE] = b;
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
		return back.get(index/SLICE_SIZE)[index % SLICE_SIZE];
	}

	public synchronized long getLong(int idx) {
		long accum = 0;
		for(int i = 0; i <= 60; i += 4)
			accum |= get(idx+i) << 60-i;
		return accum;
	}

	public void close() {
		try{
			for(byte[] barr : back)
				dout.write(barr);
			dout.close();
		}catch(IOException e){
			Util.fatalError("Could not write out file",e);
		}
	}

}
