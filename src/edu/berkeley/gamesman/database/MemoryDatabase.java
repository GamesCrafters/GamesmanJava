package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;

/**
 * Stores the entire database as an array of bytes
 * 
 * @author dnspies
 */
public class MemoryDatabase extends DatabaseWrapper {

	public MemoryDatabase(Database db, String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords) {
		this(db, uri, conf, solve, firstRecord, numRecords, true);
	}

	public MemoryDatabase(Database db, String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords,
			boolean backChanges) {
		super(db, uri, conf, solve, firstRecord, numRecords);
		this.myFirstRecord = super.firstRecord();
		this.myNumRecords = super.numRecords();
		memoryStorage = new byte[(int) numBytes(firstRecord(), numRecords())];
		firstByte = toByte(this.myFirstRecord);
		firstNum = toNum(firstRecord());
		numBytes = (int) (lastByte(firstRecord() + numRecords()) - firstByte);
		lastNum = toNum(firstRecord() + numRecords());
		this.backChanges = backChanges;
		if (backChanges) {
			myHandle = db.getHandle();
			db.getRecordsAsBytes(myHandle, firstByte, firstNum, memoryStorage,
					0, numBytes, lastNum, true);
		} else
			myHandle = null;
	}

	protected byte[] memoryStorage;

	private final DatabaseHandle myHandle;

	private long firstByte;

	private int firstNum;

	private int numBytes;

	private int lastNum;

	private long myFirstRecord;

	private long myNumRecords;

	private final boolean backChanges;

	@Override
	protected void closeDatabase() {
		flush();
		db.closeHandle(myHandle);
		db.close();
	}

	@Override
	public void flush() {
		if (solve && myHandle != null) {
			db.putRecordsAsBytes(myHandle, firstByte, firstNum, memoryStorage,
					0, numBytes, lastNum, false);
		}
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		System.arraycopy(memoryStorage, (int) (loc - firstByte), arr, off, len);
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		System.arraycopy(arr, off, memoryStorage, (int) (loc - firstByte), len);
	}

	@Override
	protected long getRecordsAsLongGroup(DatabaseHandle dh, long byteIndex,
			int firstNum, int lastNum) {
		return longRecordGroup(memoryStorage, (int) (byteIndex - firstByte));
	}

	@Override
	protected long getLongRecordGroup(DatabaseHandle dh, long loc) {
		return longRecordGroup(memoryStorage, (int) (loc - firstByte));
	}

	@Override
	protected BigInteger getRecordsAsBigIntGroup(DatabaseHandle dh,
			long byteIndex, int firstNum, int lastNum) {
		return bigIntRecordGroup(memoryStorage, (int) (byteIndex - firstByte));
	}

	@Override
	protected BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		return bigIntRecordGroup(memoryStorage, (int) (loc - firstByte));
	}

	public void setRange(long firstRecord, int numRecords) {
		this.myFirstRecord = firstRecord;
		this.myNumRecords = numRecords;
		firstByte = toByte(firstRecord);
		firstNum = toNum(firstRecord);
		numBytes = (int) (lastByte(firstRecord + numRecords) - firstByte);
		lastNum = toNum(firstRecord + numRecords);
		if (memoryStorage == null || memoryStorage.length < numBytes)
			memoryStorage = new byte[numBytes];
		if (backChanges) {
			db.getRecordsAsBytes(myHandle, firstByte, firstNum, memoryStorage,
					0, numBytes, lastNum, true);
		}
	}

	@Override
	public long firstRecord() {
		return myFirstRecord;
	}

	@Override
	public long numRecords() {
		return myNumRecords;
	}
}
