package edu.berkeley.gamesman.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import edu.berkeley.gamesman.util.GZIPOutputStream;

/**
 * A stream which zips into chunks prefaced by their size and then writes out to
 * the underlying stream. Call nextChunk() to finish a chunk and write it out.
 * It will return the number of bytes which were written.
 * 
 * @author dnspies
 */
public class ZipChunkOutputStream extends FilterOutputStream {
	private final ChunkOutputStream cos;
	private final GZIPOutputStream gzos;
	private boolean bytesWritten;
	private boolean onChunk = false;

	/**
	 * @param out
	 *            The underlying stream to write chunks to
	 * @param bufferSize
	 *            The size of the GZIP buffer to use
	 * @throws IOException
	 *             If an IO Error occurs while instantiating
	 */
	public ZipChunkOutputStream(OutputStream out, final int bufferSize)
			throws IOException {
		this(out, bufferSize, true);
	}

	/**
	 * @param out
	 *            An underlying stream to write zipped chunks to
	 * @param bufferSize
	 *            The size of the GZIP buffer to use
	 * @param initialize
	 *            Whether to initialize the first chunk in the constructor (if
	 *            not, you must call startChunk() before you can begin writing)
	 * @throws IOException
	 *             If an IOException occurs while opening the stream
	 */
	public ZipChunkOutputStream(OutputStream out, int bufferSize,
			boolean initialize) throws IOException {
		super(new ChunkOutputStream(out));
		cos = (ChunkOutputStream) this.out;
		gzos = new GZIPOutputStream(cos, bufferSize);
		if (initialize) {
			onChunk = true;
		} else {
			cos.clearChunk();
		}
	}

	/**
	 * Resets this object as if it had just been instantiated
	 * 
	 * @param initialize
	 *            Whether to initialize the first chunk (if not, you must call
	 *            startChunk() before you can begin writing)
	 * @throws IOException
	 *             If an IOException occurs while opening the stream
	 */
	public void renew(boolean initialize) throws IOException {
		bytesWritten = false;
		onChunk = false;
		cos.renew();
		gzos.renew();
		if (initialize) {
			onChunk = true;
		} else {
			cos.clearChunk();
		}
	}

	public void write(int b) throws IOException {
		if (!onChunk)
			throw new IOException("No current chunk! Use startChunk() first");
		gzos.write(b);
		bytesWritten = true;
	}

	public void write(byte b[]) throws IOException {
		if (!onChunk)
			throw new IOException("No current chunk! Use startChunk() first");
		gzos.write(b);
		bytesWritten = true;
	}

	public void write(byte b[], int off, int len) throws IOException {
		if (!onChunk)
			throw new IOException("No current chunk! Use startChunk() first");
		gzos.write(b, off, len);
		bytesWritten = true;
	}

	/**
	 * Finishes the current chunk and writes it to the underlying stream, but
	 * does not start a new one.
	 * 
	 * @return The number of bytes written to the underlying stream
	 * @throws IOException
	 *             If an IO Error occurs
	 */
	public int finishAndWriteChunk() throws IOException {
		if (!onChunk)
			throw new IOException("No current chunk! Use startChunk() first");
		finishChunk();
		onChunk = false;
		return cos.nextChunk();
	}

	/**
	 * Writes the current chunk out to the underlying stream and starts a new
	 * chunk
	 * 
	 * @return The number of bytes written
	 * @throws IOException
	 *             If an IOException occurs while writing
	 */
	public int nextChunk() throws IOException {
		if (!onChunk)
			throw new IOException(
					"No chunk to be written back!  Use startChunk() instead");
		int result = finishAndWriteChunk();
		startChunk();
		return result;
	}

	/**
	 * Starts a new chunk (assuming the previous chunk has already been finished
	 * and written)
	 * 
	 * @throws IOException
	 *             If an IOError occurs
	 */
	public void startChunk() throws IOException {
		if (onChunk)
			throw new IOException(
					"Already writing a chunk! Use finishAndWriteChunk() first");
		gzos.renew();
		bytesWritten = false;
		onChunk = true;
	}

	@Override
	public void close() throws IOException {
		if (bytesWritten) {
			if (onChunk)
				finishAndWriteChunk();
			super.close();
		} else {
			clearChunkAndClose();
		}
	}

	/**
	 * Finishes writing compressed data to the output stream without closing the
	 * underlying stream. Use this method when applying multiple filters in
	 * succession to the same output stream.
	 * 
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	public void finish() throws IOException {
		if (bytesWritten) {
			if (onChunk)
				finishAndWriteChunk();
			cos.finish();
		} else {
			clearChunkAndFinish();
		}
	}

	/**
	 * Clears the current chunk instead of writing it and finishes this stream
	 * without closing the underlying stream
	 */
	public void clearChunkAndFinish() {
		cos.clearChunk();
	}

	/**
	 * Clears the current chunk instead of writing it and closes the underlying
	 * stream
	 * 
	 * @throws IOException
	 *             If an IOException occurs while closing the stream
	 */
	public void clearChunkAndClose() throws IOException {
		cos.clearChunkAndClose();
	}

	/**
	 * Clears the current chunk and starts a new one
	 * 
	 * @throws IOException
	 *             If an IOException occurs while starting the new chunk.
	 */
	public void clearChunk() throws IOException {
		if (bytesWritten) {
			cos.clearChunk();
			onChunk = false;
			startChunk();
		}
	}

	/**
	 * Finishes zipping the current chunk, but does not write it out to the
	 * underlying stream
	 * 
	 * @throws IOException
	 *             If an IOException is thrown while zipping
	 */
	public void finishChunk() throws IOException {
		gzos.finish();
	}
}
