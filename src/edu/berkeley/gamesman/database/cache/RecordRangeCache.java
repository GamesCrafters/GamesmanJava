package edu.berkeley.gamesman.database.cache;

import java.io.EOFException;
import java.io.IOException;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.DatabaseLogic;
import edu.berkeley.gamesman.database.UnpreparedHandleException;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

public class RecordRangeCache {
	private byte[] recordBytes = new byte[0];
	private long firstRecordIndex;
	private int numRecords;
	private long firstByteIndex;
	private int numBytes;
	private final DatabaseLogic myLogic;

	public RecordRangeCache(DatabaseLogic logic) {
		myLogic = logic;
	}

	public RecordRangeCache(Database db) {
		myLogic = db.myLogic;
	}

	public void setRange(long firstRecordIndex, int numRecords) {
		this.firstRecordIndex = firstRecordIndex;
		ensureCapacity(numRecords, false);
		this.numRecords = numRecords;
		firstByteIndex = myLogic.getByteIndex(firstRecordIndex);
		numBytes = (int) myLogic.getNumBytes(numRecords);
	}

	public void ensureCapacity(int numRecords, boolean saveCurrent) {
		ensureByteCapacity((int) myLogic.getNumBytes(numRecords), saveCurrent);
	}

	public void ensureByteCapacity(int numBytes, boolean saveCurrent) {
		if (recordBytes.length < numBytes) {
			if (!saveCurrent) {
				recordBytes = null;
				System.gc();
			}
			byte[] newBytes;
			try {
				newBytes = new byte[numBytes];
			} catch (OutOfMemoryError e) {
				System.out.println(Runtime.getRuntime().totalMemory()
						+ " bytes currently from "
						+ Runtime.getRuntime().maxMemory() + " bytes possible");
				throw e;
			}
			if (saveCurrent)
				System.arraycopy(recordBytes, 0, newBytes, 0, this.numBytes);
			recordBytes = newBytes;
			if (saveCurrent)
				System.gc();
		}
	}

	public int readBytes(long location, byte[] array, int off, int len) {
		System.arraycopy(recordBytes, (int) (location - firstByteIndex), array,
				off, len);
		return len;
	}

	public int writeBytes(long location, byte[] array, int off, int len) {
		System.arraycopy(array, off, recordBytes,
				(int) (location - firstByteIndex), len);
		return len;
	}

	public long readRecord(long recordIndex) {
		return readRecordFromByteIndex(myLogic.getByteIndex(recordIndex));
	}

	public long readRecordFromByteIndex(long byteIndex) {
		return myLogic.getRecord(recordBytes,
				(int) (byteIndex - firstByteIndex));
	}

	public void writeRecord(long recordIndex, long record) {
		writeRecordFromByteIndex(myLogic.getByteIndex(recordIndex), record);
	}

	public void writeRecordFromByteIndex(long byteIndex, long record) {
		myLogic.fillBytes(record, recordBytes,
				(int) (byteIndex - firstByteIndex));
	}

	public long getFirstRecordIndex() {
		return firstRecordIndex;
	}

	public int getNumRecords() {
		return numRecords;
	}

	public void readRecordsFromDatabase(Database db, DatabaseHandle dh,
			long recordIndex, int numRecords) throws IOException {
		Util.debug(DebugFacility.CACHE, "Reading records " + recordIndex + "-"
				+ (recordIndex + numRecords - 1) + " from database");
		readBytesFromDatabase(db, dh, myLogic.getByteIndex(recordIndex),
				(int) myLogic.getNumBytes(numRecords));
	}

	public void readBytesFromDatabase(Database db, DatabaseHandle dh,
			long byteIndex, int numBytes) throws IOException {
		db.prepareReadRange(dh, byteIndex, numBytes);
		try {
			readSequentialBytesFromDatabase(db, dh, byteIndex, numBytes);
		} catch (UnpreparedHandleException e) {
			throw new Error(e);
		} catch (EOFException e) {
			throw new Error(e);
		}
	}

	protected void readSequentialBytesFromDatabase(Database db,
			DatabaseHandle dh, long byteIndex, int numBytes) throws IOException {
		db.readFullBytes(dh, recordBytes, (int) (byteIndex - firstByteIndex),
				numBytes);
	}

	public void writeRecordsToDatabase(Database db, DatabaseHandle dh,
			long recordIndex, int numRecords) throws IOException {
		assert Util.debug(DebugFacility.CACHE, "Writing records " + recordIndex
				+ "-" + (recordIndex + numRecords - 1) + " to database");
		writeBytesToDatabase(db, dh, myLogic.getByteIndex(recordIndex),
				(int) myLogic.getNumBytes(numRecords));
	}

	public void writeBytesToDatabase(Database db, DatabaseHandle dh,
			long byteIndex, int numBytes) throws IOException {
		db.prepareWriteRange(dh, byteIndex, numBytes);
		try {
			writeSequentialBytesToDatabase(db, dh, byteIndex, numBytes);
		} catch (UnpreparedHandleException e) {
			throw new Error(e);
		} catch (EOFException e) {
			throw new Error(e);
		}
	}

	protected void writeSequentialBytesToDatabase(Database db,
			DatabaseHandle dh, long byteIndex, int numBytes) throws IOException {
		db.writeFullBytes(dh, recordBytes, (int) (byteIndex - firstByteIndex),
				numBytes);
	}

	public long getFirstByteIndex() {
		return firstByteIndex;
	}

	public int getNumBytes() {
		return numBytes;
	}

	public boolean containsRecord(long hash) {
		long place = hash - firstRecordIndex;
		return place >= 0 && place < numRecords;
	}
}
