package edu.berkeley.gamesman.database;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

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
	
	public static MemoryDatabase md;

	/* Class Variables */
	private byte[] memoryStorage; // byte array to store the data

	protected byte[] rawRecord;

	protected boolean open; // whether this database is initialized

	// and not closed.

	/**
	 * Null Constructor, used primarily for testing. It doesn't set anything
	 * 
	 * @author Alex Trofimov
	 */
	public MemoryDatabase() {
		md=this;
	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		for (int i = 0; i < conf.recordGroupByteLength; i++) {
			rawRecord[i] = getByte(loc++);
		}
		return new RecordGroup(conf, rawRecord);
	}

	@Override
	public Iterator<RecordGroup> getRecordGroups(final long loc,
			final int numGroups) {
		return new Iterator<RecordGroup>() {
			private long location = loc;

			private int groupNumber = 0;

			public boolean hasNext() {
				return groupNumber < numGroups;
			}

			public RecordGroup next() {
				RecordGroup rg = getRecordGroup(location);
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
		rawRecord = new byte[conf.recordGroupByteLength];
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup value) {
		value.writeToUnsignedMemoryDatabase(this, loc,
				conf.recordGroupByteLength);
	}

	public void putRecordGroups(long loc, Iterator<RecordGroup> it,
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

	/**
	 * Get a byte from the database.
	 * 
	 * @author Alex Trofimov
	 * @param index
	 *            sequential number of this byte in DB.
	 * @return - one byte at specified byte index.
	 */
	protected byte getByte(long index) {
		return this.memoryStorage[(int) index];
	}

	/**
	 * Write a byte into the database. Assumes that space is already allocated.
	 * 
	 * @author Alex Trofimov
	 * @param index
	 *            sequential number of byte in DB.
	 * @param data
	 *            byte that needs to be written.
	 */
	public synchronized void putByte(long index, byte data) {
		this.memoryStorage[(int) index] = data;
	}
}
