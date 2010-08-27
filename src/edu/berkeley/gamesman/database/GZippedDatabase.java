package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.ZipChunkInputStream;

/**
 * A super-class for the two GZippedDatabase classes. One uses more memory, but
 * operates quicker, the other is slower but uses less memory
 * 
 * @author dnspies
 */
public abstract class GZippedDatabase extends Database {

	// For reading and writing

	/**
	 * The default constructor
	 * 
	 * @param uri
	 *            The name of the file
	 * @param conf
	 *            The configuration object
	 * @param solve
	 *            Whether solving (If true, opened as read/write, otherwise just
	 *            write)
	 * @param firstRecord
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The number of records contained in this database
	 * @param header
	 *            The header
	 * @throws IOException
	 *             If opening the file or reading the header throws an
	 *             IOException
	 */
	public GZippedDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, DatabaseHeader header)
			throws IOException {
		super(uri, conf, solve, firstRecord, numRecords, header);
		myFile = new File(uri);
		entrySize = conf.getInteger("gamesman.db.zip.entryKB", 64) << 10;
		firstByteIndex = toByte(firstContainedRecord);
		firstEntry = firstByteIndex / entrySize;
	}

	protected final File myFile;

	protected FileInputStream fis;

	protected final int entrySize;

	protected final long firstEntry;

	protected final long firstByteIndex;

	protected GZipHandle lastUsed;

	protected final class GZipHandle extends DatabaseHandle {
		ChunkWrapInputStream cwis;
		ZipChunkInputStream zcis;
		long filePos, remainingBytes;

		public GZipHandle(int recordGroupByteLength) {
			super(recordGroupByteLength);
		}
	}

	@Override
	public GZipHandle getHandle() {
		return new GZipHandle(recordGroupByteLength);
	}

	@Override
	protected synchronized void getBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected synchronized void getRecordsAsBytes(DatabaseHandle dh,
			long byteIndex, int recordNum, byte[] arr, int off, int numBytes,
			int lastNum, boolean overwriteEdgesOk) {
		super.getRecordsAsBytes(dh, byteIndex, recordNum, arr, off, numBytes,
				lastNum, overwriteEdgesOk);
	}

	@Override
	protected synchronized int getBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean overwriteEdgesOk) {
		if (!overwriteEdgesOk)
			return super.getBytes(dh, arr, off, maxLen, false);
		final int numBytes = (int) Math.min(maxLen, dh.lastByteIndex
				- dh.location);
		try {
			GZipHandle gzh = (GZipHandle) dh;
			if (lastUsed != gzh) {
				if (lastUsed != null)
					lastUsed.filePos = fis.getChannel().position();
				lastUsed = gzh;
				fis.getChannel().position(gzh.filePos);
			}
			readFully(gzh.zcis, arr, off, numBytes);
		} catch (IOException e) {
			throw new Error(e);
		}
		dh.location += numBytes;
		return numBytes;
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		GZipHandle gzh = (GZipHandle) dh;
		long thisEntry = byteIndex / entrySize;
		int numEntries = (int) ((byteIndex + numBytes + entrySize - 1)
				/ entrySize - thisEntry);
		try {
			if (lastUsed != null)
				try {
					lastUsed.filePos = fis.getChannel().position();
				} catch (IOException e) {
					throw new Error(e);
				}
			lastUsed = gzh;
			long[] entryPoints = getEntryPoints(thisEntry, numEntries);
			gzh.filePos = entryPoints[0];
			fis.getChannel().position(gzh.filePos);
			gzh.cwis = new ChunkWrapInputStream(fis, entryPoints, 0);
			gzh.zcis = new ZipChunkInputStream(gzh.cwis);
			long curLoc;
			if (thisEntry == firstEntry)
				curLoc = firstByteIndex;
			else
				curLoc = thisEntry * entrySize;
			while (curLoc < byteIndex)
				curLoc += gzh.zcis.skip(byteIndex - curLoc);
		} catch (IOException e) {
			throw new Error(e);
		}
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
	}

	protected abstract long[] getEntryPoints(long firstEntry, int numEntries);

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final long getSize() {
		return myFile.length();
	}
}

/**
 * Wraps a stream using the location table to determine where the GZipped chunks
 * are. Think of it as the inverse of ChunkInputStream.
 * 
 * @author dnspies
 */
final class ChunkWrapInputStream extends FilterInputStream {
	final long[] positions;
	int nextEntry;
	long curPos;
	int lengthBytes = 4;

	ChunkWrapInputStream(InputStream in, long[] positions, int curEntry) {
		super(in);
		this.positions = positions;
		nextEntry = curEntry + 1;
		curPos = positions[curEntry];
	}

	@Override
	public int read() throws IOException {
		if (nextEntry == positions.length)
			return -1;
		int blockBytes = (int) (positions[nextEntry] - curPos);
		if (blockBytes + lengthBytes == 0) {
			nextEntry++;
			if (nextEntry == positions.length)
				return -1;
			blockBytes = (int) (positions[nextEntry] - curPos);
			lengthBytes = 4;
		}
		if (lengthBytes > 0) {
			lengthBytes--;
			return (blockBytes >> (lengthBytes << 3)) & 255;
		} else {
			curPos++;
			return in.read();
		}
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		if (nextEntry == positions.length)
			return -1;
		int blockBytes = (int) (positions[nextEntry] - curPos);
		int totalBytesRead = 0;
		if (blockBytes + lengthBytes == 0) {
			nextEntry++;
			if (nextEntry == positions.length)
				return -1;
			blockBytes = (int) (positions[nextEntry] - curPos);
			lengthBytes = 4;
		}
		while (lengthBytes > 0 && len > 0) {
			lengthBytes--;
			arr[off++] = (byte) (blockBytes >> (lengthBytes << 3));
			len--;
			totalBytesRead++;
		}
		if (len > 0) {
			int bytesRead = in.read(arr, off, Math.min(len, blockBytes));
			totalBytesRead += bytesRead;
			curPos += bytesRead;
		}
		return totalBytesRead;
	}

	@Override
	public long skip(long n) throws IOException {
		if (nextEntry == positions.length)
			return -1;
		int blockBytes = (int) (positions[nextEntry] - curPos);
		int totalBytesSkipped = 0;
		if (blockBytes + lengthBytes == 0) {
			nextEntry++;
			if (nextEntry == positions.length)
				return -1;
			blockBytes = (int) (positions[nextEntry] - curPos);
			lengthBytes = 4;
		}
		int lengthSkip = (int) Math.min(lengthBytes, n);
		if (lengthSkip > 0) {
			lengthBytes -= lengthSkip;
			n -= lengthSkip;
			totalBytesSkipped += lengthSkip;
		}
		if (n > 0) {
			int bytesSkipped = (int) in.skip(Math.min(n, blockBytes));
			totalBytesSkipped += bytesSkipped;
			curPos += bytesSkipped;
		}
		return totalBytesSkipped;
	}
}
