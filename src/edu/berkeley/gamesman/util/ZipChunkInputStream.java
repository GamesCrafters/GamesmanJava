package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class ZipChunkInputStream extends FilterInputStream {
	private final ChunkInputStream cis;
	private final UncloseableInputStream unclose;

	public ZipChunkInputStream(InputStream in) throws IOException {
		super(in);
		cis = new ChunkInputStream(in);
		unclose = new UncloseableInputStream(cis);
		this.in = new GZIPInputStream(unclose);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(unclose);
			bytesRead = in.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		int byteRead = in.read();
		if (byteRead < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(unclose);
			byteRead = in.read();
		}
		return byteRead;
	}

	@Override
	public long skip(long n) throws IOException {
		long bytesSkipped = in.skip(n);
		if (bytesSkipped < 0) {
			cis.nextChunk();
			in = new GZIPInputStream(unclose);
			bytesSkipped = in.skip(n);
		}
		return bytesSkipped;
	}

	@Override
	public void close() throws IOException {
		cis.close();
	}
}
