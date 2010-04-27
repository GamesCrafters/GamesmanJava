package edu.berkeley.gamesman.database;

import java.math.BigInteger;
import java.util.Iterator;

import edu.berkeley.gamesman.util.LongIterator;

/**
 * Like MemoryDatabase, but synchronized
 * 
 * @author dnspies
 */
public class SynchronizedMemoryDatabase extends MemoryDatabase {

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		super.putRecordGroups(loc, recordGroups, numGroups);
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		super.putRecordGroups(loc, recordGroups, numGroups);
	}

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
