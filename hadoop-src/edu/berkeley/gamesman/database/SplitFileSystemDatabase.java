package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class SplitFileSystemDatabase extends FileSystemDatabase {
	private final Database[] databaseList;
	private final long[] firstByteIndices;
	private final int[] firstNums;
	private final long[] lastByteIndices;
	private final int[] lastNums;

	public SplitFileSystemDatabase(Path uri, FSDataInputStream is, FileSystem fs) {
		super(uri, is);
		Scanner dbStream = new Scanner(is);
		ArrayList<GZippedFileSystemDatabase> databaseList = new ArrayList<GZippedFileSystemDatabase>();
		String dbType = dbStream.next();
		while (!dbType.equals("end")) {
			Path dbUri = new Path(dbStream.next());
			long dbFirstRecord = dbStream.nextLong();
			long dbNumRecords = dbStream.nextLong();
			try {
				databaseList.add(new GZippedFileSystemDatabase(dbUri,
						dbFirstRecord, dbNumRecords, fs.open(dbUri)));
			} catch (IOException e) {
				throw new Error(e);
			}
			dbType = dbStream.next();
		}
		this.databaseList = databaseList.toArray(new Database[databaseList
				.size()]);
		firstByteIndices = new long[databaseList.size()];
		firstNums = new int[databaseList.size()];
		lastByteIndices = new long[databaseList.size()];
		lastNums = new int[databaseList.size()];
		for (int i = 0; i < lastByteIndices.length; i++) {
			Database db = this.databaseList[i];
			long dbFirstRecord = db.firstRecord();
			firstByteIndices[i] = toByte(dbFirstRecord);
			firstNums[i] = toNum(dbFirstRecord);
			long dbLastRecord = dbFirstRecord + db.numRecords();
			lastByteIndices[i] = lastByte(dbLastRecord);
			lastNums[i] = toNum(dbLastRecord);
		}
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		SplitHandle sh = (SplitHandle) dh;
		sh.currentDb = binSearch(byteIndex, firstNum);
		long firstNumBytes = lastByteIndices[sh.currentDb] - byteIndex;
		int firstLastNum;
		if (numBytes < firstNumBytes) {
			firstNumBytes = numBytes;
			firstLastNum = lastNum;
		} else if (numBytes > firstNumBytes) {
			firstLastNum = lastNums[sh.currentDb];
		} else {
			firstLastNum = Math.min(lastNum, lastNums[sh.currentDb]);
		}
		databaseList[sh.currentDb].prepareRange(sh.handles[sh.currentDb],
				byteIndex, firstNum, firstNumBytes, firstLastNum);
		sh.dbLoc[sh.currentDb] = byteIndex;
	}

	@Override
	protected int getBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen,
			boolean overwriteEdgesOk) {
		if (!overwriteEdgesOk) {
			return super.getBytes(dh, arr, off, maxLen, false);
		}
		final int numBytes = (int) Math.min(dh.lastByteIndex - dh.location,
				maxLen);
		SplitHandle sh = (SplitHandle) dh;
		while (sh.location >= lastByteIndices[sh.currentDb])
			sh.dbLoc[sh.currentDb++] = -1;
		int db = sh.currentDb;
		while (db < firstByteIndices.length
				&& firstByteIndices[db] < sh.location + numBytes) {
			if (sh.dbLoc[db] < 0) {
				long lastByteIndex = lastByteIndices[db];
				int lastNum;
				if (lastByteIndex > sh.lastByteIndex) {
					lastByteIndex = sh.lastByteIndex;
					lastNum = sh.lastNum;
				} else if (lastByteIndex < sh.lastByteIndex) {
					lastNum = lastNums[db];
				} else
					lastNum = Math.min(sh.lastNum, lastNums[db]);
				if (firstByteIndices[db] < lastByteIndex
						- recordGroupByteLength
						|| lastNum == 0 || firstNums[db] < lastNum) {
					databaseList[db].prepareRange(sh.handles[db],
							firstByteIndices[db], firstNums[db], lastByteIndex
									- firstByteIndices[db], lastNum);
					sh.dbLoc[db] = firstByteIndices[db];
					if (sh.dbLoc[db] < sh.location) {
						byte[] skipBytes = dh.getRecordGroupBytes();
						sh.dbLoc[db] += databaseList[db].getBytes(
								sh.handles[db], skipBytes, 0,
								(int) (sh.location - sh.dbLoc[db]), true);
						dh.releaseBytes(skipBytes);
					}
				} else
					break;
			}
			sh.dbLoc[db] += databaseList[db].getBytes(sh.handles[db], arr, off
					+ (int) (sh.dbLoc[db] - sh.location), numBytes
					- (int) (sh.dbLoc[db] - sh.location), false);
			db++;
		}
		sh.location += numBytes;
		if (sh.lastByteIndex == sh.location) {
			for (int i = sh.currentDb; i < sh.dbLoc.length && sh.dbLoc[i] >= 0; i++)
				sh.dbLoc[i] = -1;
		}
		return numBytes;
	}

	private int binSearch(long byteIndex, int num) {
		int low = 0, high = databaseList.length;
		while (high - low > 1) {
			int guess = (low + high) >>> 1;
			if (firstByteIndices[guess] > byteIndex
					|| (firstByteIndices[guess] == byteIndex && firstNums[guess] > num))
				high = guess;
			else
				low = guess;
		}
		return low;
	}

	private class SplitHandle extends DatabaseHandle {
		public final DatabaseHandle[] handles;
		public int currentDb;
		public final long[] dbLoc;

		public SplitHandle(int recordGroupByteLength) {
			super(recordGroupByteLength);
			handles = new DatabaseHandle[databaseList.length];
			dbLoc = new long[databaseList.length];
			for (int i = 0; i < handles.length; i++) {
				handles[i] = databaseList[i].getHandle();
				dbLoc[i] = -1;
			}
		}
	}

	@Override
	public DatabaseHandle getHandle() {
		return new SplitHandle(recordGroupByteLength);
	}

	@Override
	protected void closeDatabase() {
		for (Database d : databaseList) {
			d.close();
		}
	}
}
