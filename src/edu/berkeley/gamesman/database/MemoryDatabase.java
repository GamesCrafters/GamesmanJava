package edu.berkeley.gamesman.database;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * Test DataBase for GamesCrafters Java. Right now it just writes BigIntegers to
 * memory, without byte padding.
 * 
 * 
 * @author Alex Trofimov
 * @version 1.4
 * 
 *          Change log: 05/05/09 - 1.4 - putByte() is now synchronized, for
 *          multi-threading. This is really important. 03/20/09 - 1.3 - With
 *          data sizes < 58 bits, longs are used instead of BigInts, 20%
 *          speedup. 03/15/09 - 1.2 - Slight speedup for operating on small data
 *          (< 8 bits); ensureCapacity() added. 02/22/09 - 1.1 - Switched to a
 *          byte[] instead of ArrayList<Byte> for internal storage. 02/21/09 -
 *          1.0 - Initial (working) Version.
 */
public class MemoryDatabase extends Database {

	/**
	 * Set this variable to a MemoryDatabase when instantiated so it can be used
	 * by a testing class later.
	 */
	public static MemoryDatabase md;

	/* Class Variables */
	private byte[] memoryStorage; // byte array to store the data

	protected boolean open; // whether this database is initialized

	// and not closed.

	private int nextPlace = 0;

	/**
	 * Null Constructor, used primarily for testing. It doesn't set anything
	 * 
	 * @author Alex Trofimov
	 */
	public MemoryDatabase() {
		md = this;
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
	public Iterator<BigInteger> getBigIntRecordGroups(final long loc,
			final int numGroups) {
		return new Iterator<BigInteger>() {
			private long location = loc;

			private int groupNumber = 0;

			public boolean hasNext() {
				return groupNumber < numGroups;
			}

			public BigInteger next() {
				BigInteger rg = getBigIntRecordGroup(location);
				location += conf.recordGroupByteLength;
				groupNumber++;
				return rg;
			}

			public void remove() {
				throw new UnsupportedOperationException(
						"remove() not implemented");
			}
		};
	}

	@Override
	public LongIterator getLongRecordGroups(final long loc, final int numGroups) {
		return new LongIterator() {
			private long location = loc;

			private int groupNumber = 0;

			public boolean hasNext() {
				return groupNumber < numGroups;
			}

			public long next() {
				long rg = getLongRecordGroup(location);
				location += conf.recordGroupByteLength;
				groupNumber++;
				return rg;
			}
		};
	}

	@Override
	public void initialize(String locations) {
		this.initialize();
	}

	/**
	 * Null Constructor for testing the database outside of Gamesman
	 * environment. Initializes the internal storage.
	 * 
	 * @author Alex Trofimov
	 */
	private void initialize() {
		System.out.println(getByteSize());
		this.memoryStorage = new byte[(int) getByteSize()];
		this.open = true;
	}

	@Override
	public synchronized void putRecordGroup(long loc, long value) {
		RecordGroup.toUnsignedByteArray(conf, value, memoryStorage, (int) loc);
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger value) {
		RecordGroup.toUnsignedByteArray(conf, value, memoryStorage, (int) loc);
	}

	@Override
	public synchronized void putRecordGroups(long loc, LongIterator it,
			int numGroups) {
		for (int i = 0; i < numGroups; i++) {
			putRecordGroup(loc, it.next());
			loc += conf.recordGroupByteLength;
		}
	}

	@Override
	public synchronized void putRecordGroups(long loc, Iterator<BigInteger> it,
			int numGroups) {
		for (int i = 0; i < numGroups; i++) {
			putRecordGroup(loc, it.next());
			loc += conf.recordGroupByteLength;
		}
	}

	@Override
	public void flush() {
		assert Util.debug(DebugFacility.DATABASE,
				"Flushing Memory DataBase. Does Nothing.");
	}

	@Override
	public void close() {
		this.open = false;
		flush();
		assert Util.debug(DebugFacility.DATABASE,
				"Closing Memory DataBase. Does Nothing.");
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			arr[off++] = memoryStorage[nextPlace++];
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			memoryStorage[nextPlace++] = arr[off++];
	}

	@Override
	public void seek(long loc) {
		nextPlace = (int) loc;
	}
}
