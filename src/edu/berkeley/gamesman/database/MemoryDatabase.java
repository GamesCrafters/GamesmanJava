package edu.berkeley.gamesman.database;

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
		memoryStorage = new byte[(int) numBytes(firstRecord(), numRecords())];
		firstByte = toByte(firstRecord);
		if (backChanges) {
			myHandle = db.getHandle(firstRecord(), numRecords());
			db.getRecordsAsBytes(myHandle, firstRecord(), memoryStorage, 0,
					(int) numRecords(), true);
		} else
			myHandle = null;
	}

	protected final byte[] memoryStorage;

	private final DatabaseHandle myHandle;

	private final long firstByte;

	@Override
	protected void closeDatabase() {
		finish();
		db.close();
	}

	public void finish() {
		if (solve && myHandle != null) {
			db.putRecordsAsBytes(myHandle, firstRecord(), memoryStorage, 0,
					(int) numRecords(), false);
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
}
