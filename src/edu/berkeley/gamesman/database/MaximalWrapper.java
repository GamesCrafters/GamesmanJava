package edu.berkeley.gamesman.database;

public class MaximalWrapper extends DatabaseWrapper {

	public MaximalWrapper(Database db) {
		super(db);
	}

	@Override
	protected void initialize(String uri, boolean solve) {
		db.initialize(uri, solve);
	}

	@Override
	public void flush() {
		db.flush();
	}

	@Override
	public void close() {
		db.close();
	}

	@Override
	public long getRecord(DatabaseHandle dh, long recordIndex) {
		return db.getRecord(dh, recordIndex);
	}

	@Override
	public void putRecord(DatabaseHandle dh, long recordIndex, long r) {
		db.putRecord(dh, recordIndex, r);
	}

	@Override
	protected void putRecordsAsBytes(DatabaseHandle dh, long recordIndex,
			byte[] arr, int off, int numRecords) {
		db.putRecordsAsBytes(dh, recordIndex, arr, off, numRecords);
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		db.putBytes(dh, loc, arr, off, len);
	}

	@Override
	protected void getRecordsAsBytes(DatabaseHandle dh, long recordIndex,
			byte[] arr, int off, int numRecords, boolean overwriteOk) {
		db
				.getRecordsAsBytes(dh, recordIndex, arr, off, numRecords,
						overwriteOk);
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		db.getBytes(dh, loc, arr, off, len);
	}

	@Override
	protected synchronized void seek(long loc) {
		db.seek(loc);
	}

	@Override
	protected synchronized void putBytes(byte[] arr, int off, int len) {
		db.putBytes(arr, off, len);
	}

	@Override
	protected synchronized void getBytes(byte[] arr, int off, int len) {
		db.getBytes(arr, off, len);
	}

	@Override
	public void fill(long r, long offset, long len) {
		db.fill(r, offset, len);
	}

	@Override
	public long numRecords() {
		return db.numRecords();
	}

	@Override
	public void setRange(long firstByte, long numBytes) {
		db.setRange(firstByte, numBytes);
	}

	@Override
	public long firstRecord() {
		return db.firstRecord();
	}

	@Override
	public DatabaseHandle getHandle() {
		return db.getHandle();
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		db.closeHandle(dh);
	}

	@Override
	public DatabaseHandle getHandle(long recordStart, long numRecords) {
		return db.getHandle(recordStart, numRecords);
	}

	@Override
	public long[] splitRange(long firstRecord, long numRecords, int numSplits) {
		return db.splitRange(firstRecord, numRecords, numSplits);
	}
}
