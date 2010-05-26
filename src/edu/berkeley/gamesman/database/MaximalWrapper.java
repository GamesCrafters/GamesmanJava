package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;

public class MaximalWrapper extends DatabaseWrapper {

	public MaximalWrapper(Database db, String uri, Configuration config,
			boolean solve, long firstRecord, long numRecords) {
		super(db, uri, config, solve, firstRecord, numRecords);
	}

	@Override
	public void flush() {
		db.flush();
	}

	@Override
	protected void closeDatabase() {
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
	protected long getLongRecordGroup(DatabaseHandle dh, long loc) {
		return db.getLongRecordGroup(dh, loc);
	}

	@Override
	protected BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		return db.getBigIntRecordGroup(dh, loc);
	}

	@Override
	protected void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		db.putRecordGroup(dh, loc, rg);
	}

	@Override
	protected void putRecordGroup(DatabaseHandle dh, long loc, BigInteger rg) {
		db.putRecordGroup(dh, loc, rg);
	}

	@Override
	protected void putRecordsAsBytes(DatabaseHandle dh, long recordIndex,
			byte[] arr, int off, int numRecords, boolean overwriteEdgesOk) {
		db.putRecordsAsBytes(dh, recordIndex, arr, off, numRecords,
				overwriteEdgesOk);
	}

	@Override
	protected void putRecordsAsGroup(DatabaseHandle dh, long recordIndex,
			int numRecords, long rg) {
		db.putRecordsAsGroup(dh, recordIndex, numRecords, rg);
	}

	@Override
	protected void putRecordsAsGroup(DatabaseHandle dh, long recordIndex,
			int numRecords, BigInteger rg) {
		db.putRecordsAsGroup(dh, recordIndex, numRecords, rg);
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		db.putBytes(dh, loc, arr, off, len);
	}

	@Override
	protected void getRecordsAsBytes(DatabaseHandle dh, long recordIndex,
			byte[] arr, int off, int numRecords, boolean overwriteEdgesOk) {
		db.getRecordsAsBytes(dh, recordIndex, arr, off, numRecords,
				overwriteEdgesOk);
	}

	@Override
	protected long getRecordsAsLongGroup(DatabaseHandle dh, long recordIndex,
			int numRecords) {
		return db.getRecordsAsLongGroup(dh, recordIndex, numRecords);
	}

	@Override
	protected BigInteger getRecordsAsBigIntGroup(DatabaseHandle dh,
			long recordIndex, int numRecords) {
		return db.getRecordsAsBigIntGroup(dh, recordIndex, numRecords);
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
	
	@Override
	public long numRecords() {
		return db.numRecords();
	}

	@Override
	public long firstRecord() {
		return db.firstRecord();
	}
}
