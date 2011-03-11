package edu.berkeley.gamesman.database;

import java.io.EOFException;
import java.io.IOException;

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

	protected int readBytes(long location, byte[] array, int off, int len) {
		System.arraycopy(recordBytes, (int) (location - firstByteIndex), array,
				off, len);
		return len;
	}

	protected int writeBytes(long location, byte[] array, int off, int len) {
		System.arraycopy(array, off, recordBytes,
				(int) (location - firstByteIndex), len);
		return len;
	}

	public long readRecord(long recordIndex) {
		return readRecordFromByteIndex(myLogic.getByteIndex(recordIndex));
	}

	protected long readRecordFromByteIndex(long byteIndex) {
		return myLogic.getRecord(recordBytes,
				(int) (byteIndex - firstByteIndex));
	}

	public void writeRecord(long recordIndex, long record) {
		writeRecordFromByteIndex(myLogic.getByteIndex(recordIndex), record);
	}

	protected void writeRecordFromByteIndex(long byteIndex, long record) {
		myLogic.fillBytes(record, recordBytes,
				(int) (byteIndex - firstByteIndex));
	}

	public long getFirstRecordIndex() {
		return firstRecordIndex;
	}

	public int getNumRecords() {
		return numRecords;
	}

	protected void readFromDatabase(Database db, DatabaseHandle dh,
			long byteIndex, int numBytes) throws IOException {
		db.prepareReadRange(dh, byteIndex, numBytes);
		try {
			readSequentialFromDatabase(db, dh, byteIndex, numBytes);
		} catch (UnpreparedHandleException e) {
			throw new Error(e);
		} catch (EOFException e) {
			throw new Error(e);
		}
	}

	protected void readSequentialFromDatabase(Database db, DatabaseHandle dh,
			long byteIndex, int numBytes) throws IOException {
		db.readFullBytes(dh, recordBytes, (int) (byteIndex - firstByteIndex),
				numBytes);
	}

	public void writeRecordsToDatabase(Database db, DatabaseHandle dh,
			long recordIndex, int numRecords) throws IOException {
		writeBytesToDatabase(db, dh, myLogic.getByteIndex(recordIndex),
				(int) myLogic.getNumBytes(numRecords));
	}

	protected void writeBytesToDatabase(Database db, DatabaseHandle dh,
			long byteIndex, int numBytes) throws IOException {
		db.prepareWriteRange(dh, byteIndex, numBytes);
		try {
			writeSequentialToDatabase(db, dh, byteIndex, numBytes);
		} catch (UnpreparedHandleException e) {
			throw new Error(e);
		} catch (EOFException e) {
			throw new Error(e);
		}
	}

	protected void writeSequentialToDatabase(Database db, DatabaseHandle dh,
			long byteIndex, int numBytes) throws IOException {
		db.writeFullBytes(dh, recordBytes, (int) (byteIndex - firstByteIndex),
				numBytes);
	}

	protected long getFirstByteIndex() {
		return firstByteIndex;
	}

	protected int getNumBytes() {
		return numBytes;
	}

	public boolean containsRecord(long hash) {
		long place = hash - firstRecordIndex;
		return place >= 0 && place < numRecords;
	}
}
