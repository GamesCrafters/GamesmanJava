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
			boolean solve, boolean backChanges) {
		this(db, uri, conf, solve, 0, 0, backChanges, true);
	}

	public MemoryDatabase(Database db, String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords,
			boolean backChanges) {
		this(db, uri, conf, solve, firstRecord, numRecords, backChanges, false);
	}

	private MemoryDatabase(Database db, String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords,
			boolean backChanges, boolean mutable) {
		super(db, uri, conf, solve, firstRecord, numRecords);
		firstRecord = this.myFirstRecord = super.firstRecord();
		numRecords = this.myNumRecords = super.numRecords();
		this.backChanges = backChanges;
		this.mutable = mutable;
		if (backChanges)
			myHandle = db.getHandle();
		else
			myHandle = null;
		if (!mutable && backChanges) {
			firstByte = toByte(firstRecord);
			numBytes = (int) (lastByte(firstRecord + numRecords) - firstByte);
			memoryStorage = new byte[numBytes];
			firstNum = toNum(firstRecord);
			lastNum = toNum(firstRecord + numRecords);
			db.getRecordsAsBytes(myHandle, firstByte, firstNum, memoryStorage,
					0, numBytes, lastNum, true);
		}
	}

	protected byte[] memoryStorage;

	private final DatabaseHandle myHandle;

	private long firstByte;

	private int firstNum;

	private int numBytes;

	private int lastNum;

	private long myFirstRecord;

	private long myNumRecords;

	private final boolean backChanges, mutable;

	@Override
	public void close() {
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

	@Override
	protected void putRecordGroup(DatabaseHandle dh, long loc, long r) {
		toUnsignedByteArray(r, memoryStorage, (int) (loc - firstByte));
	}

	@Override
	protected void putRecordsAsGroup(DatabaseHandle dh, long byteIndex,
			int firstNum, int lastNum, long r) {
		long group1 = getLongRecordGroup(dh, byteIndex);
		long group3 = group1;
		r = splice(group1, r, firstNum);
		r = splice(r, group3, lastNum);
		toUnsignedByteArray(r, memoryStorage, (int) (byteIndex - firstByte));
	}

	@Override
	protected void putRecordGroup(DatabaseHandle dh, long loc, BigInteger r) {
		toUnsignedByteArray(r, memoryStorage, (int) (loc - firstByte));
	}

	@Override
	protected void putRecordsAsGroup(DatabaseHandle dh, long byteIndex,
			int firstNum, int lastNum, BigInteger r) {
		BigInteger group1 = getBigIntRecordGroup(dh, byteIndex);
		BigInteger group3 = group1;
		r = splice(group1, r, firstNum);
		r = splice(r, group3, lastNum);
		toUnsignedByteArray(r, memoryStorage, (int) (byteIndex - firstByte));
	}

	public void setRange(long firstRecord, int numRecords) {
		if (!mutable)
			throw new UnsupportedOperationException();
		this.myFirstRecord = firstRecord;
		this.myNumRecords = numRecords;
		firstByte = toByte(firstRecord);
		firstNum = toNum(firstRecord);
		numBytes = (int) (lastByte(firstRecord + numRecords) - firstByte);
		lastNum = toNum(firstRecord + numRecords);
		ensureByteSize(numBytes);
		if (backChanges) {
			db.getRecordsAsBytes(myHandle, firstByte, firstNum, memoryStorage,
					0, numBytes, lastNum, true);
		}
	}

	// Does not preserve stored records.
	public void ensureByteSize(int numBytes) {
		if (memoryStorage == null || memoryStorage.length < numBytes)
			memoryStorage = new byte[numBytes];
	}

	@Override
	public long firstRecord() {
		return myFirstRecord;
	}

	@Override
	public long numRecords() {
		return myNumRecords;
	}

	// TODO Do this without cheating
	@Override
	public long getRecord(DatabaseHandle dh, long recordIndex) {
		if (!superCompress && recordGroupByteLength == 1)
			return memoryStorage[(int) (recordIndex - firstByte)];
		else
			return super.getRecord(dh, recordIndex);
	}

	// TODO Do this without cheating
	@Override
	public void putRecord(DatabaseHandle dh, long recordIndex, long r) {
		if (!superCompress && recordGroupByteLength == 1)
			memoryStorage[(int) (recordIndex - firstByte)] = (byte) r;
		else
			super.putRecord(dh, recordIndex, r);
	}
}
