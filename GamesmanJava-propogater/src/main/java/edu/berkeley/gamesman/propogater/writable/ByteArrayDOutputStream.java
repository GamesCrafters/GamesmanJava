package edu.berkeley.gamesman.propogater.writable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;

public class ByteArrayDOutputStream extends ByteArrayOutputStream {
	public synchronized void writeTo(DataOutput out) throws IOException {
		out.write(buf, 0, count);
	}

	public synchronized void writeTo(byte[] b, int off) {
		System.arraycopy(buf, 0, b, off, count);
	}
}
