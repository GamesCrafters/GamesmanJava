package edu.berkeley.gamesman.database;

public class DatabaseWrapper extends Database {
	Database db;

	public DatabaseWrapper(Database db) {
		this.db = db;
	}

	@Override
	public void close() {
		db.close();
	}

	@Override
	public void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		db.getBytes(dh, loc, arr, off, len);
	}

	@Override
	public void initialize(String uri, boolean solve) {
		db.initialize(uri, solve);
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		db.putBytes(dh, loc, arr, off, len);
	}

	@Override
	public DatabaseHandle getHandle(long recordStart, long numRecords) {
		return db.getHandle(recordStart, numRecords);
	}

	@Override
	public DatabaseHandle getHandle() {
		return db.getHandle();
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		db.closeHandle(dh);
	}
}
