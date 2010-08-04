package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class ZipChunkInputStream extends FilterInputStream {
	private final ChunkInputStream cis;

	public ZipChunkInputStream(InputStream in) throws IOException {
		super(in);
		cis = new ChunkInputStream(in);
		this.in = new GZIPInputStream(cis);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(cis);
			bytesRead = in.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		int byteRead = in.read();
		if (byteRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(cis);
			byteRead = in.read();
		}
		return byteRead;
	}

	@Override
	public long skip(long n) throws IOException {
		long bytesSkipped = in.skip(n);
		if (bytesSkipped < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(cis);
			bytesSkipped = in.skip(n);
		}
		return bytesSkipped;
	}
}
