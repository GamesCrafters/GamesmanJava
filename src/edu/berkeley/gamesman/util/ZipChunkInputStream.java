package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class ZipChunkInputStream extends FilterInputStream {
	private final ChunkInputStream cis;
	private final UncloseableInputStream unclose;
	private final int bufferSize;

	public ZipChunkInputStream(InputStream in, int bufferSize) throws IOException {
		super(in);
		this.bufferSize = bufferSize;
		cis = new ChunkInputStream(in);
		unclose = new UncloseableInputStream(cis);
		this.in = new GZIPInputStream(unclose, bufferSize);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(unclose, bufferSize);
			bytesRead = in.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		int byteRead = in.read();
		if (byteRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(unclose, bufferSize);
			byteRead = in.read();
		}
		return byteRead;
	}

	@Override
	public long skip(long n) throws IOException {
		long bytesSkipped = in.skip(n);
		if (bytesSkipped < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(unclose, bufferSize);
			bytesSkipped = in.skip(n);
		}
		return bytesSkipped;
	}

	@Override
	public void close() throws IOException {
		cis.close();
	}
}
