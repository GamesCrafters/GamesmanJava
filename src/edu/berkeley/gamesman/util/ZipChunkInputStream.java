package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

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
	private final GZIPInputStream gzin;
	private boolean closed = false;

	/**
	 * @param in
	 *            The underlying input stream to read from
	 * @param bufferSize
	 *            The size of the buffer to use while unpacking zipped chunks
	 * @throws IOException
	 *             If an IOException occurs while creating the stream
	 */
	public ZipChunkInputStream(InputStream in, int bufferSize)
			throws IOException {
		super(in);
		cis = new ChunkInputStream(in);
		unclose = new UncloseableInputStream(cis);
		this.in = gzin = new GZIPInputStream(unclose, bufferSize);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		if (closed)
			throw new IOException("Stream closed");
		int bytesRead = in.read(arr, off, len);
		if (bytesRead < 0) {
			cis.nextChunk();
			gzin.renew();
			bytesRead = in.read(arr, off, len);
		}
		return bytesRead;
	}

	@Override
	public int read() throws IOException {
		if (closed)
			throw new IOException("Stream closed");
		int byteRead = in.read();
		if (byteRead < 0) {
			cis.nextChunk();
			gzin.renew();
			byteRead = in.read();
		}
		return byteRead;
	}

	@Override
	public long skip(long n) throws IOException {
		if (closed)
			throw new IOException("Stream closed");
		long bytesSkipped = in.skip(n);
		if (bytesSkipped < 0) {
			cis.nextChunk();
			gzin.renew();
			bytesSkipped = in.skip(n);
		}
		return bytesSkipped;
	}

	@Override
	public void close() throws IOException {
		finish();
		cis.close();
	}

	/**
	 * Finish reading from the underlying stream, but don't close it
	 * 
	 * @throws IOException
	 *             If an IOException occurs
	 */
	public void finish() throws IOException {
		in.close();
		closed = true;
	}

	/**
	 * Reset this object as if it had just been created
	 * 
	 * @throws IOException
	 *             If an IOException occurs
	 */
	public void renew() throws IOException {
		closed = false;
		cis.renew();
		gzin.renew();
	}
}
