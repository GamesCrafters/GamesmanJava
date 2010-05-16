package edu.berkeley.gamesman.database;

import java.math.BigInteger;

/**
 * Like MemoryDatabase, but synchronized
 * 
 * @author dnspies
 */
public class SynchronizedMemoryDatabase extends MemoryDatabase {

	@Override
	public synchronized void putRecordGroup(long loc, long rg) {
		super.putRecordGroup(loc, rg);
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger rg) {
		super.putRecordGroup(loc, rg);
	}

	@Override
	public synchronized void putBytes(long loc, byte[] arr, int off, int len) {
		super.putBytes(loc, arr, off, len);
	}
}
