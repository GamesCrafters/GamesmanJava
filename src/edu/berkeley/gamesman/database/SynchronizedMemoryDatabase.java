package edu.berkeley.gamesman.database;

import java.math.BigInteger;

/**
 * Like MemoryDatabase, but synchronized
 * 
 * @author dnspies
 */
public class SynchronizedMemoryDatabase extends MemoryDatabase {

	@Override
	public synchronized void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		super.putRecordGroup(dh, loc, rg);
	}

	@Override
	public synchronized void putRecordGroup(DatabaseHandle dh, long loc,
			BigInteger rg) {
		super.putRecordGroup(dh, loc, rg);
	}

	@Override
	public synchronized void putBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len) {
		super.putBytes(dh, loc, arr, off, len);
	}
}
