package edu.berkeley.gamesman.database;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ZipChunkInputStream extends FilterInputStream {
	private final ChunkInputStream cis;
	private GZIPInputStream gzi;

	public ZipChunkInputStream(InputStream in) throws IOException {
		super(in);
		cis = new ChunkInputStream(in);
		gzi = new GZIPInputStream(cis);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		int bytesRead = gzi.read(arr, off, len);
		if (bytesRead < 0) {
			cis.nextChunk();
			gzi = new GZIPInputStream(cis);
			bytesRead = gzi.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		int byteRead = gzi.read();
		if (byteRead < 0) {
			cis.nextChunk();
			gzi = new GZIPInputStream(cis);
			byteRead = gzi.read();
		}
		return byteRead;
	}
}
