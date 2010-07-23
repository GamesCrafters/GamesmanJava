package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UndeterminedChunkInputStream extends FilterInputStream {
	int remainBytes;

	public UndeterminedChunkInputStream(InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		if (remainBytes == 0)
			nextChunk();
		if (remainBytes < 0)
			return -1;
		remainBytes--;
		return in.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (remainBytes == 0)
			nextChunk();
		if (remainBytes < 0)
			return -1;
		int bytesRead = in.read(b, off, Math.min(len, remainBytes));
		remainBytes -= bytesRead;
		return bytesRead;
	}

	@Override
	public long skip(long n) throws IOException {
		if (remainBytes == 0)
			nextChunk();
		if (remainBytes < 0)
			return -1L;
		long bytesSkipped = in.skip(Math.min(n, remainBytes));
		remainBytes -= bytesSkipped;
		return bytesSkipped;
	}

	public void nextChunk() throws IOException {
		remainBytes = 0;
		for (int i = 0; i < 4; i++) {
			remainBytes <<= 8;
			remainBytes |= in.read() & 255;
		}
	}

	public void skipToChunkEnd() throws IOException {
		if (remainBytes == 0)
			nextChunk();
		while (remainBytes >= 0) {
			skip(remainBytes);
			if (remainBytes == 0)
				nextChunk();
		}
	}

	public void finish() throws IOException {
		skipToChunkEnd();
	}
}
