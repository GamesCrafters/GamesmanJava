package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.berkeley.gamesman.util.qll.Pool;

/**
 * Reads from a stream which contains zipped chunks (packed by a
 * ZipChunkOutputStream)
 * 
 * @see ZipChunkOutputStream
 * 
 * @author dnspies
 */
public final class ZipChunkInputStream extends FilterInputStream {
	private final ChunkInputStream cis;
	private final UncloseableInputStream unclose;
	private final Pool<byte[]> bytePool;
	private boolean closed = false;

	/**
	 * @param in
	 *            The underlying input stream to read from
	 * @param bufferSize
	 *            The size of the buffer to use while unpacking zipped chunks
	 * @throws IOException
	 *             If an IOException occurs while creating the stream
	 */
	public ZipChunkInputStream(InputStream in, Pool<byte[]> bytePool)
			throws IOException {
		super(in);
		cis = new ChunkInputStream(in);
		unclose = new UncloseableInputStream(cis);
		this.bytePool = bytePool;
		this.in = new GZIPInputStream(unclose, bytePool);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		if(closed)
			throw new IOException("Stream closed");
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0) {
			in.close();
			cis.nextChunk();
			in = new GZIPInputStream(unclose, bytePool);
			bytesRead = in.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		if(closed)
			throw new IOException("Stream closed");
		int byteRead = in.read();
		if (byteRead < 0) {
			in.close();
			cis.nextChunk();
			in = new GZIPInputStream(unclose, bytePool);
			byteRead = in.read();
		}
		return byteRead;
	}

	@Override
	public long skip(long n) throws IOException {
		if(closed)
			throw new IOException("Stream closed");
		long bytesSkipped = in.skip(n);
		if (bytesSkipped < 0) {
			in.close();
			cis.nextChunk();
			in = new GZIPInputStream(unclose, bytePool);
			bytesSkipped = in.skip(n);
		}
		return bytesSkipped;
	}

	@Override
	public void close() throws IOException {
		finish();
		cis.close();
	}

	public void finish() throws IOException {
		in.close();
		closed = true;
	}
}
