package edu.berkeley.gamesman.database;

import java.math.BigInteger;

public class MaximalWrapper extends DatabaseWrapper {

	public MaximalWrapper(Database db) {
		super(db);
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
	public long getLongRecordGroup(DatabaseHandle dh, long loc) {
		return db.getLongRecordGroup(dh, loc);
	}

	@Override
	public BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		return db.getBigIntRecordGroup(dh, loc);
	}

	@Override
	public void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		db.putRecordGroup(dh, loc, rg);
	}

	@Override
	public void putRecordGroup(DatabaseHandle dh, long loc, BigInteger rg) {
		db.putRecordGroup(dh, loc, rg);
	}

	@Override
	public void seek(long loc) {
		db.seek(loc);
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		db.putBytes(arr, off, len);
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		db.getBytes(arr, off, len);
	}

	@Override
	public void fill(long r, long offset, long len) {
		db.fill(r, offset, len);
	}

	@Override
	public long getByteSize() {
		return db.getByteSize();
	}

	@Override
	public void setRange(long firstByte, long numBytes) {
		db.setRange(firstByte, numBytes);
	}

	@Override
	public long firstByte() {
		return db.firstByte();
	}
}
