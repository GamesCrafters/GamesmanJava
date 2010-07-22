package edu.berkeley.gamesman.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UndeterminedChunkOutputStream extends FilterOutputStream {
	private final byte[] bufferedBytes;
	int curByte = 0;

	public UndeterminedChunkOutputStream(OutputStream out) {
		this(out, 2048);
	}

	public UndeterminedChunkOutputStream(OutputStream out, int bufferSize) {
		super(out);
		bufferedBytes = new byte[bufferSize];
	}

	@Override
	public synchronized void write(int b) throws IOException {
		if (curByte == bufferedBytes.length) {
			for (int i = 24; i >= 0; i -= 8) {
				out.write(bufferedBytes.length >>> i);
			}
			out.write(bufferedBytes);
			curByte = 0;
		}
		bufferedBytes[curByte++] = (byte) b;
	}

	@Override
	public synchronized void write(byte[] b, int off, int len)
			throws IOException {
		if (curByte + len >= bufferedBytes.length) {
			int numBytes = curByte + len;
			for (int i = 24; i >= 0; i -= 8)
				out.write(numBytes >>> i);
			out.write(bufferedBytes, 0, curByte);
			out.write(b, off, len);
			curByte = 0;
		} else {
			System.arraycopy(b, off, bufferedBytes, curByte, len);
			curByte += len;
		}
	}

	@Override
	public synchronized void flush() throws IOException {
		if (curByte > 0) {
			for (int i = 24; i >= 0; i -= 8)
				out.write(curByte >>> i);
			out.write(bufferedBytes, 0, curByte);
			curByte = 0;
		}
		out.flush();
	}

	public synchronized void nextChunk() throws IOException {
		if (curByte > 0) {
			for (int i = 24; i >= 0; i -= 8)
				out.write(curByte >>> i);
			out.write(bufferedBytes, 0, curByte);
			curByte = 0;
		}
		for (int i = 0; i < 4; i++)
			out.write(-1);
	}
}
