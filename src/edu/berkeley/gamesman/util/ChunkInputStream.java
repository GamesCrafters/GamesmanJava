package edu.berkeley.gamesman.util;

import java.io.DataInput;
import java.io.DataInputStream;
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
public final class ChunkInputStream extends FilterInputStream {
	private int remain;
	private DataInput dis;

	public ChunkInputStream(InputStream in) throws IOException {
		super(in instanceof DataInput ? in : new DataInputStream(in));
		dis = (DataInput) this.in;
		nextChunk();
	}

	public void nextChunk() throws IOException {
		remain = dis.readInt();
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		if (remain <= 0)
			return -1;
		len = Math.min(len, remain);
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0)
			return -1;
		remain -= bytesRead;
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		if (remain <= 0)
			return -1;
		int byteRead = in.read();
		if (byteRead < 0)
			return -1;
		remain--;
		return byteRead;
	}
}
