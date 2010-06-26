package edu.berkeley.gamesman.database;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ChunkInputStream extends FilterInputStream {
	private int remain = 0;
	private final byte[] numBytesBytes = new byte[4];

	public ChunkInputStream(InputStream in) throws IOException {
		super(in);
		nextChunk();
	}

	public void nextChunk() throws IOException {
		Database.readFully(in, numBytesBytes, 0, 4);
		remain = 0;
		for (int i = 0; i < 4; i++) {
			remain <<= 8;
			remain |= numBytesBytes[i] & 255;
		}
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		if (remain == 0)
			return -1;
		len = Math.min(len, remain);
		int bytesRead = super.read(arr, off, len);
		if (bytesRead < 0)
			return -1;
		remain -= bytesRead;
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		if (remain == 0)
			return -1;
		int byteRead = super.read();
		if (byteRead < 0)
			return -1;
		remain--;
		return byteRead;
	}
}
