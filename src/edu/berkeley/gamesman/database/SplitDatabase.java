package edu.berkeley.gamesman.database;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import edu.berkeley.gamesman.core.Configuration;

public abstract class SplitDatabase extends Database {
	public static class DatabaseDescriptor implements
			Comparable<DatabaseDescriptor> {
		public DatabaseDescriptor(String dbClass, String uri,
				long firstRecordIndex, long numRecords) {
			this.dbClass = dbClass;
			this.uri = uri;
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
		}

		public final String dbClass;
		public final String uri;
		public final long firstRecordIndex;
		public final long numRecords;

		@Override
		public int compareTo(DatabaseDescriptor o) {
			if (this.firstRecordIndex < o.firstRecordIndex)
				return -1;
			else if (this.firstRecordIndex > o.firstRecordIndex)
				return 1;
			else if (this.numRecords < o.numRecords)
				return -1;
			else if (this.numRecords > o.numRecords)
				return 1;
			else
				return 0;
		}
	}

	private final Database[] containedDbs;
	private final long[] rangeByteIndexStarts;

	public SplitDatabase(DataInputStream dis, String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, ClassNotFoundException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		skipHeader(dis);
		LinkedList<DatabaseDescriptor> dbList = new LinkedList<DatabaseDescriptor>();
		long lastRecordIndex = firstRecordIndex + numRecords;
		DatabaseDescriptor lastDescriptor = null;
		long lastLastRecordIndex = 0L;
		while (true) {
			DatabaseDescriptor nextDescriptor;
			try {
				nextDescriptor = new DatabaseDescriptor(dis.readUTF(),
						dis.readUTF(), dis.readLong(), dis.readLong());
			} catch (EOFException e) {
				break;
			}
			assert lastDescriptor == null
					|| nextDescriptor.firstRecordIndex == lastLastRecordIndex;
			long nextLastRecordIndex = nextDescriptor.firstRecordIndex
					+ nextDescriptor.numRecords;
			if (nextDescriptor.firstRecordIndex < lastRecordIndex
					&& nextLastRecordIndex > firstRecordIndex)
				dbList.add(nextDescriptor);
			lastDescriptor = nextDescriptor;
			lastLastRecordIndex = nextLastRecordIndex;
		}
		dis.close();
		containedDbs = new Database[dbList.size()];
		rangeByteIndexStarts = new long[dbList.size() + 1];
		int i = 0;
		for (DatabaseDescriptor db : dbList) {
			containedDbs[i] = newDatabase(db);
			rangeByteIndexStarts[i] = myLogic.getByteIndex(db.firstRecordIndex);
			i++;
		}
		rangeByteIndexStarts[i] = myLogic.getByteIndex(lastRecordIndex);
	}

	private Database newDatabase(DatabaseDescriptor db) throws IOException,
			ClassNotFoundException {
		return Database.openDatabase(db.dbClass, db.uri, conf,
				db.firstRecordIndex, db.numRecords, reading, writing);
	}

	private class SplitHandle extends DatabaseHandle {
		private final DatabaseHandle[] innerHandles;
		private int currentDb = -1;

		public SplitHandle(int numBytes, boolean reading) {
			super(numBytes, reading);
			innerHandles = new DatabaseHandle[containedDbs.length];
		}

	}

	@Override
	public DatabaseHandle getHandle(boolean reading) {
		assert reading ? this.reading : this.writing;
		return new SplitHandle(myLogic.recordBytes, reading);
	}

	@Override
	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		long endByteIndex = firstByteIndex + numBytes;
		int firstDb = findDb(firstByteIndex);
		int lastDb = findDb(endByteIndex - 1);
		SplitHandle sh = (SplitHandle) dh;
		for (int dbNum = firstDb; dbNum <= lastDb; dbNum++) {
			DatabaseHandle dh2 = getInnerHandle(sh, dbNum);
			long innerFirst = dbNum == firstDb ? firstByteIndex
					: rangeByteIndexStarts[dbNum];
			long innerNum = (dbNum == lastDb ? endByteIndex
					: rangeByteIndexStarts[dbNum + 1]) - innerFirst;
			containedDbs[dbNum].prepareReadRange(dh2, innerFirst, innerNum);
		}
		sh.currentDb = firstDb;
	}

	private DatabaseHandle getInnerHandle(SplitHandle sh, int dbNum) {
		DatabaseHandle dh2 = sh.innerHandles[dbNum];
		if (dh2 == null) {
			dh2 = containedDbs[dbNum].getHandle(sh.reading);
			sh.innerHandles[dbNum] = dh2;
		}
		return dh2;
	}

	private int findDb(long byteIndex) {
		int db = Arrays.binarySearch(rangeByteIndexStarts, byteIndex);
		if (db < 0) {
			db = -db - 2;
		}
		return db;
	}

	@Override
	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		long endByteIndex = firstByteIndex + numBytes;
		int firstDb = findDb(firstByteIndex);
		int lastDb = findDb(endByteIndex - 1);
		SplitHandle sh = (SplitHandle) dh;
		for (int dbNum = firstDb; dbNum <= lastDb; dbNum++) {
			DatabaseHandle dh2 = getInnerHandle(sh, dbNum);
			long innerFirst = dbNum == firstDb ? firstByteIndex
					: rangeByteIndexStarts[dbNum];
			long innerNum = (dbNum == lastDb ? endByteIndex
					: rangeByteIndexStarts[dbNum + 1]) - innerFirst;
			containedDbs[dbNum].prepareWriteRange(dh2, innerFirst, innerNum);
		}
		sh.currentDb = firstDb;
	}

	@Override
	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		SplitHandle sh = (SplitHandle) dh;
		long nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		while (sh.location == nextStop) {
			sh.currentDb++;
			nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		}
		int toRead = (int) Math.min(maxLen, nextStop - sh.location);
		return containedDbs[sh.currentDb].readBytes(
				sh.innerHandles[sh.currentDb], array, off, toRead);
	}

	@Override
	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		SplitHandle sh = (SplitHandle) dh;
		long nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		while (sh.location == nextStop) {
			sh.currentDb++;
			nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		}
		int toWrite = (int) Math.min(maxLen, nextStop - sh.location);
		return containedDbs[sh.currentDb].writeBytes(
				sh.innerHandles[sh.currentDb], array, off, toWrite);
	}

	@Override
	public void close() throws IOException {
		for (Database db : containedDbs) {
			db.close();
		}
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		throw new UnsupportedOperationException();
	}
}
