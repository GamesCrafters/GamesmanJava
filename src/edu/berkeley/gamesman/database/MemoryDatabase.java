package edu.berkeley.gamesman.database;

import java.math.BigInteger;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;

/**
 * Stores the entire database as an array of bytes
 * 
 * @author dnspies
 */
public class MemoryDatabase extends Database {
	/* Class Variables */
	protected byte[] memoryStorage; // byte array to store the data

	protected boolean readingOnly;

	private int nextPlace = 0;

	@Override
	public void initialize(String location, boolean solve) {
		maxBytes = (int) getByteSize();
		memoryStorage = new byte[maxBytes];
	}

	@Override
	public void flush() {
		assert Util.debug(DebugFacility.DATABASE,
				"Flushing Memory DataBase. Does Nothing.");
	}

	@Override
	public void close() {
	}

	/**
	 * Must be synchronized by caller
	 */
	@Override
	public void getBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			arr[off++] = memoryStorage[nextPlace++];
	}

	/**
	 * Must be synchronized by caller
	 */
	@Override
	public void putBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			memoryStorage[nextPlace++] = arr[off++];
	}

	@Override
	public void seek(long loc) {
		nextPlace = (int) loc;
	}

	@Override
	public synchronized long getLongRecordGroup(long loc) {
		return RecordGroup.longRecordGroup(conf, memoryStorage, (int) loc);
	}

	@Override
	public synchronized BigInteger getBigIntRecordGroup(long loc) {
		return RecordGroup.bigIntRecordGroup(conf, memoryStorage, (int) loc);
	}

	@Override
	public Iterator<BigInteger> getBigIntRecordGroups(long loc, int numGroups) {
		return new BigIntRecordGroupByteIterator(memoryStorage, (int) loc,
				numGroups);
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		return new LongRecordGroupByteIterator(memoryStorage, (int) loc,
				numGroups);
	}

	@Override
	public void putRecordGroup(long loc, long rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage, (int) loc);
	}

	@Override
	public void putRecordGroup(long loc, BigInteger rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage, (int) loc);
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		for (int i = 0; i < numGroups; i++) {
			RecordGroup.toUnsignedByteArray(conf, recordGroups.next(),
					memoryStorage, (int) loc);
			loc += conf.recordGroupByteLength;
		}
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		for (int i = 0; i < numGroups; i++) {
			RecordGroup.toUnsignedByteArray(conf, recordGroups.next(),
					memoryStorage, (int) loc);
			loc += conf.recordGroupByteLength;
		}
	}
}
