package edu.berkeley.gamesman.database;

import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.ZipChunkInputStream;

/**
 * A file database whose records are GZipped in chunks. Each chunk contains
 * gamesman.db.zip.entryKB (default 64) raw kilobytes of records (record groups
 * may be split across chunk boundaries). The first numEntries*8 bytes after the
 * header is a list of offsets into the file at which each chunk begins. This is
 * loaded into an array when the database is opened and held in memory until it
 * is garbage collected.
 * 
 * @author dnspies
 */
public class GZippedFileSystemDatabase extends Database {
	private static class Info {
		public Info(FileSystem fs, String uri, long firstRecord, long numRecords)
				throws IOException {
			this.fs = fs;
			this.uri = uri;
			this.solve = false;
			this.firstRecord = firstRecord;
			this.numRecords = numRecords;
			in = fs.open(new Path(uri));
			byte[] headBytes = new byte[18];
			in.readFully(headBytes);
			this.header = new DatabaseHeader(headBytes);
			try {
				conf = Configuration.load(in);
			} catch (ClassNotFoundException e) {
				throw new Error(e);
			}
		}

		private final String uri;
		private final Configuration conf;
		private final boolean solve;
		private final long firstRecord;
		private final long numRecords;
		private final DatabaseHeader header;
		private final FileSystem fs;
		private final FSDataInputStream in;
	}

	private final int numEntries;

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

	private final FileStatus myFile;

	protected FSDataInputStream fis;

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
					lastUsed.filePos = fis.getPos();
				lastUsed = gzh;
				fis.seek(gzh.filePos);
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
					lastUsed.filePos = fis.getPos();
				} catch (IOException e) {
					throw new Error(e);
				}
			lastUsed = gzh;
			long[] entryPoints = getEntryPoints(thisEntry, numEntries);
			gzh.filePos = entryPoints[0];
			fis.seek(gzh.filePos);
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

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final long getSize() {
		return myFile.getLen();
	}

	@Override
	public void close() {
		try {
			fis.close();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		super.closeHandle(dh);
	}

	/**
	 * The default constructor
	 * 
	 * @param uri
	 *            The name of the file
	 * @param conf
	 *            The configuration object
	 * @param solve
	 *            Should be false
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
	public GZippedFileSystemDatabase(FileSystem fs, String uri,
			long firstRecord, long numRecords) throws IOException {
		this(new Info(fs, uri, firstRecord, numRecords));
	}

	private GZippedFileSystemDatabase(Info info) throws IOException {
		super(info.uri, info.conf, info.solve, info.firstRecord,
				info.numRecords, info.header);
		Path uriPath = new Path(info.uri);
		myFile = info.fs.getFileStatus(uriPath);
		entrySize = conf.getInteger("gamesman.db.zip.entryKB", 64) << 10;
		firstByteIndex = toByte(firstContainedRecord);
		firstEntry = firstByteIndex / entrySize;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries + 1];
		fis = info.in;
		byte[] entryBytes = new byte[entryPoints.length << 3];
		readFully(fis, entryBytes, 0, entryBytes.length);
		int count = 0;
		for (int i = 0; i < entryPoints.length; i++) {
			for (int bit = 56; bit >= 0; bit -= 8) {
				entryPoints[i] <<= 8;
				entryPoints[i] |= ((int) entryBytes[count++]) & 255;
			}
			if (i > 0 && entryPoints[i] - entryPoints[i - 1] == 0)
				throw new EOFException("No bytes in block " + i + "/"
						+ numEntries + " (" + (i + firstEntry) + " total)");
		}
	}

	private final long[] entryPoints;

	private final long lastByteIndex;

	protected long[] getEntryPoints(long firstEntry, int numEntries) {
		long[] res = new long[numEntries + 1];
		System.arraycopy(entryPoints, (int) (firstEntry - this.firstEntry),
				res, 0, numEntries + 1);
		return res;
	}
}
