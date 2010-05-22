package edu.berkeley.gamesman.database;

import java.math.BigInteger;

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

	private DatabaseHandle myHandle;

	@Override
	public void initialize(String uri, boolean solve) {
		db.initialize(uri, conf, solve);
		memoryStorage = new byte[(int) numRecords()];
		myHandle = db.getHandle(firstRecord(), numRecords());
		long firstRecord = Math.max(firstRecord(), db.firstRecord());
		long lastRecord = Math.min(firstRecord() + numRecords(), db
				.firstRecord()
				+ db.numRecords());
		setRange(firstRecord, lastRecord - firstRecord);
		db.getBytes(myHandle, firstRecord(), memoryStorage, 0,
				(int) numRecords());
	}

	@Override
	public void close() {
		finish();
		db.close();
	}

	public void finish() {
		if (solve) {
			db.putBytes(myHandle, firstRecord(), memoryStorage, 0,
					(int) numRecords());
		}
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		System.arraycopy(memoryStorage, (int) loc, arr, off, len);
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		System.arraycopy(arr, off, memoryStorage, (int) loc, len);
	}
}
