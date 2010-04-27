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
	public synchronized long getLongRecordGroup(long loc) {
		return super.getLongRecordGroup(loc);
	}

	@Override
	public synchronized BigInteger getBigIntRecordGroup(long loc) {
		return super.getBigIntRecordGroup(loc);
	}

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
}
