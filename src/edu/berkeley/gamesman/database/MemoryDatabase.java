package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.RecordGroup;

/**
 * Stores the entire database as an array of bytes
 * 
 * @author dnspies
 */
public class MemoryDatabase extends DatabaseWrapper {

	public MemoryDatabase(Database db) {
		super(db);
	}

	protected byte[] memoryStorage;

	protected boolean readingOnly;

	DatabaseHandle myHandle;

	@Override
	public void initialize(String uri, boolean solve) {
		db.initialize(uri, conf, solve);
		memoryStorage = new byte[(int) getByteSize()];
		if (myHandle == null) {
			long firstRecord = firstByte() / conf.recordGroupByteLength
					* conf.recordsPerGroup;
			long lastRecord = (firstByte() + getByteSize()
					+ conf.recordGroupByteLength - 1)
					/ conf.recordGroupByteLength * conf.recordsPerGroup;
			myHandle = db.getHandle(firstRecord, lastRecord - firstRecord);
		}
		db.getBytes(myHandle, firstByte(), memoryStorage, 0,
				(int) getByteSize());
	}

	@Override
	public void close() {
		if (solve) {
			if (myHandle == null) {
				long firstRecord = firstByte() / conf.recordGroupByteLength
						* conf.recordsPerGroup;
				long lastRecord = (firstByte() + getByteSize()
						+ conf.recordGroupByteLength - 1)
						/ conf.recordGroupByteLength * conf.recordsPerGroup;
				myHandle = db.getHandle(firstRecord, lastRecord - firstRecord);
			}
			db.putBytes(myHandle, firstByte(), memoryStorage, 0,
					(int) getByteSize());
		}
		db.close();
	}

	@Override
	public void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		System.arraycopy(memoryStorage, (int) loc, arr, off, len);
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		System.arraycopy(arr, off, memoryStorage, (int) loc, len);
	}

	@Override
	public long getLongRecordGroup(DatabaseHandle dh, long loc) {
		return RecordGroup.longRecordGroup(conf, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		return RecordGroup.bigIntRecordGroup(conf, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public void putRecordGroup(DatabaseHandle dh, long loc, BigInteger rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage,
				(int) (loc - firstByte()));
	}
}
