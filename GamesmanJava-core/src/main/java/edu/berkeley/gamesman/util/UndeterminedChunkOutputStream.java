package edu.berkeley.gamesman.util;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UndeterminedChunkOutputStream extends FilterOutputStream {
	private final byte[] bufferedBytes;
	private int curByte = 0;
	private DataOutputStream dos;

	public UndeterminedChunkOutputStream(OutputStream out) {
		this(out, 2048);
	}

	public UndeterminedChunkOutputStream(OutputStream out, int bufferSize) {
		super(new DataOutputStream(out));
		dos = (DataOutputStream) this.out;
		bufferedBytes = new byte[bufferSize];
	}

	@Override
	public void write(int b) throws IOException {
		if (curByte == bufferedBytes.length)
			finishCurrent();
		bufferedBytes[curByte++] = (byte) b;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (curByte + len >= bufferedBytes.length) {
			int numBytes = curByte + len;
			dos.writeInt(numBytes);
			out.write(bufferedBytes, 0, curByte);
			out.write(b, off, len);
			curByte = 0;
		} else {
			System.arraycopy(b, off, bufferedBytes, curByte, len);
			curByte += len;
		}
	}

	@Override
	public void flush() throws IOException {
		finishCurrent();
		out.flush();
	}

	public void finish() throws IOException {
		nextChunk();
		out.flush();
	}

	private void finishCurrent() throws IOException {
		if (curByte > 0) {
			dos.writeInt(curByte);
			out.write(bufferedBytes, 0, curByte);
			curByte = 0;
		}
	}

	public void nextChunk() throws IOException {
		finishCurrent();
		dos.writeInt(-1);
	}
}
