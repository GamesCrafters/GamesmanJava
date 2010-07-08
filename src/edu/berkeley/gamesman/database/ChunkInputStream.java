package edu.berkeley.gamesman.database;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An underlying input stream to keep buffered input streams (such as
 * GZIPInputStream) from overstepping their bounds. It starts by reading four
 * bytes as an integer (call it n). It then reads n bytes before returning -1
 * (meaning EOF). A call to nextChunk tells the stream to read the next four
 * bytes as a length and then allow that many bytes before reaching the end
 * again. GZippedFileDatabase.readZippedBytes assumes that the reading stream
 * will be a ChunkInputStream wrapped by GZIPInputStreams. This whole system is
 * implemented by the ZipChunkInputStream.
 * 
 * @author dnspies
 */
final class ChunkInputStream extends FilterInputStream {
	private int remain;

	ChunkInputStream(InputStream in) throws IOException {
		super(in);
		nextChunk();
	}

	void nextChunk() throws IOException {
		remain = 0;
		for (int i = 0; i < 4; i++) {
			remain <<= 8;
			remain |= in.read();
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
