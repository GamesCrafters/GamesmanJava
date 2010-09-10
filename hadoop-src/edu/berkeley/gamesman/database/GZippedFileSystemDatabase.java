package edu.berkeley.gamesman.database;

import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.util.ZipChunkInputStream;

public class GZippedFileSystemDatabase extends FileSystemDatabase {
	// For reading and writing

	protected GZippedFileSystemDatabase(Path uri, long firstRecord,
			long numRecords, FSDataInputStream is) throws IOException {
		super(uri, is);
		entrySize = conf.getInteger("gamesman.db.zip.entryKB", 64) << 10;
		firstByteIndex = toByte(firstContainedRecord);
		firstEntry = firstByteIndex / entrySize;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		int numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries + 1];
		byte[] entryBytes = new byte[entryPoints.length << 3];
		readFully(is, entryBytes, 0, entryBytes.length);
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

	private final int entrySize;

	private final long firstEntry;

	private final long firstByteIndex;

	private final long lastByteIndex;

	private GZipHandle lastUsed;

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
					lastUsed.filePos = is.getPos();
				lastUsed = gzh;
				is.seek(gzh.filePos);
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
		try {
			if (lastUsed != null)
				try {
					lastUsed.filePos = is.getPos();
				} catch (IOException e) {
					throw new Error(e);
				}
			lastUsed = gzh;
			gzh.filePos = entryPoints[(int) (thisEntry - firstEntry)];
			is.seek(gzh.filePos);
			gzh.cwis = new ChunkWrapInputStream(is, entryPoints,
					(int) (thisEntry - firstEntry));
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
	protected void closeDatabase() {
	}
}
