package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class ZipChunkInputStream extends FilterInputStream {
	private final ChunkInputStream cis;
	private final int entrySize;

	public ZipChunkInputStream(InputStream in, int entrySize) throws IOException {
		super(in);
		this.entrySize = entrySize;
		cis = new ChunkInputStream(in);
		this.in = new GZIPInputStream(cis, entrySize);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(cis, entrySize);
			bytesRead = in.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		int byteRead = in.read();
		if (byteRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(cis, entrySize);
			byteRead = in.read();
		}
		return byteRead;
	}

	@Override
	public long skip(long n) throws IOException {
		long bytesSkipped = in.skip(n);
		if (bytesSkipped < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(cis, entrySize);
			bytesSkipped = in.skip(n);
		}
		return bytesSkipped;
	}
}
