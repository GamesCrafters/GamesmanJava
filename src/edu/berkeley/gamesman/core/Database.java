package edu.berkeley.gamesman.core;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * A Database is the abstract superclass of all data storage methods used in
 * Gamesman. Each particular Database is responsible for the persistent storage
 * of Records derived from solving games.
 * 
 * @author Steven Schlansker
 */
public abstract class Database {

	protected Configuration conf;

	private byte[] groups;

	private boolean solve;

	private int maxBytes;

	private long numBytes = -1;

	private long firstByte;

	/**
	 * Initialize a Database given a URI and a Configuration. This method may
	 * either open an existing database or create a new one. If a new one is
	 * created, the Configuration should be stored. If one is opened, the
	 * Configuration should be checked to ensure it matches that already stored.
	 * This method must be called exactly once before any other methods are
	 * called. The URI must be in the URI syntax, ex:
	 * file:///absolute/path/to/file.db or gdfp://server:port/dbname If config
	 * is null, it will use whatever is in the database. It is recommended that
	 * you pass in the configuration that you are expecting to ensure you don't
	 * load a db for a different game.
	 * 
	 * Note (By Alex Trofimov) I've updated the file Database to accept Relative
	 * URL, so instead of file:/// you can just put the filename, and it will
	 * create a file in the working directory (tested under windows & ubuntu).
	 * 
	 * @param uri
	 *            The URI that the Database is associated with
	 * @param config
	 *            The Configuration that is relevant
	 * @param solve
	 *            true for solving, false for playing
	 */
	public final void initialize(String uri, Configuration config, boolean solve) {
		conf = config;
		this.solve = solve;
		if (numBytes == -1) {
			firstByte = 0;
			numBytes = (conf.getGame().numHashes() + conf.recordsPerGroup - 1)
					/ conf.recordsPerGroup * conf.recordGroupByteLength;
		}
		initialize(uri, solve);
		maxBytes = 1024 - 1024 % conf.recordGroupByteLength;
		assert Util.debug(DebugFacility.DATABASE, conf.recordsPerGroup
				+ " records per group\n" + conf.recordGroupByteLength
				+ " bytes per group");
	}

	/**
	 * Initializes as above, but when the confiration is already specified
	 * 
	 * @param uri
	 *            The URI that the Database is associated with
	 * @param solve
	 *            true for solving, false for playing
	 */
	public abstract void initialize(String uri, boolean solve);

	/**
	 * Ensure all buffers are flushed to disk. The on-disk state should be
	 * consistent after this call returns.
	 */
	public abstract void flush();

	/**
	 * Close this Database, flush to disk, and release all associated resources.
	 * This object should not be used again after making this call.
	 */
	public abstract void close();

	/**
	 * Retrieve the Configuration associated with this Database.
	 * 
	 * @return the Configuration stored in the database
	 */
	public final Configuration getConfiguration() {
		return conf;
	}

	/**
	 * Notifies the database that a thread has begun writing. The solver will
	 * write to this range of records, and will be writing sequentially witin
	 * this range until recordEnd-1.
	 * 
	 * @param tier
	 *            Which tier this task belongs to (if solving all tiers at once)
	 * @param recordStart
	 *            The first record that will be written. Must be group-aligned.
	 * @param recordEnd
	 *            1 plus lastRecord
	 * @return A database that I can write this chunk to
	 */
	public Database beginWrite(int tier, long recordStart, long recordEnd) {
		return this;
	}

	/**
	 * Notifies the database that a thread has finished writing.
	 * 
	 * @param tier
	 *            Which tier this task belongs to (if solving all tiers at once)
	 * @param db
	 *            The sub-database to which the chunk was written
	 * @param recordStart
	 *            The first record that will be written. Must be group-aligned.
	 * @param recordEnd
	 *            1 plus lastRecord
	 */
	public void endWrite(int tier, Database db, long recordStart, long recordEnd) {
	}

	/**
	 * Return the Nth Record in the Database
	 * 
	 * @param recordIndex
	 *            The record number
	 * @return The stored Record
	 */
	public Record getRecord(long recordIndex) {
		Record r = conf.getGame().newRecord();
		getRecord(recordIndex, r);
		return r;
	}

	/**
	 * Store the Nth Record in the Database in provided record
	 * 
	 * @param recordIndex
	 *            The record number
	 * @param r
	 *            The record to store in
	 */
	public void getRecord(long recordIndex, Record r) {
		if (!solve)
			conf.getGame().setInterperet(recordIndex);
		if (conf.superCompress) {
			long group = recordIndex / conf.recordsPerGroup;
			int num = (int) (recordIndex % conf.recordsPerGroup);
			long byteOffset = group * conf.recordGroupByteLength;
			if (conf.recordGroupUsesLong)
				RecordGroup.getRecord(conf, getLongRecordGroup(byteOffset),
						num, r);
			else
				RecordGroup.getRecord(conf, getBigIntRecordGroup(byteOffset),
						num, r);
		} else
			RecordGroup.getRecord(conf, getLongRecordGroup(recordIndex
					* conf.recordGroupByteLength), 0, r);
	}

	/**
	 * Store a record in the Database... WARNING! NOT SYNCHRONIZED
	 * 
	 * @param recordIndex
	 *            The record number
	 * @param r
	 *            The Record to store
	 */
	public void putRecord(long recordIndex, Record r) {
		if (conf.superCompress) {
			int num = (int) (recordIndex % conf.recordsPerGroup);
			long byteOffset = recordIndex / conf.recordsPerGroup
					* conf.recordGroupByteLength;
			if (conf.recordGroupUsesLong) {
				long rg = getLongRecordGroup(byteOffset);
				rg = RecordGroup.setRecord(conf, rg, num, r);
				putRecordGroup(byteOffset, rg);
			} else {
				BigInteger rg = getBigIntRecordGroup(byteOffset);
				rg = RecordGroup.setRecord(conf, rg, num, r);
				putRecordGroup(byteOffset, rg);
			}
		} else
			putRecordGroup(recordIndex * conf.recordGroupByteLength, r
					.getState());
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	public long getLongRecordGroup(long loc) {
		int groupsLength = conf.recordGroupByteLength;
		if (groups == null || groups.length < groupsLength)
			ensureGroupsLength(groupsLength);
		// The condition avoids an unecessary synchronized method call
		getBytes(loc, groups, 0, groupsLength);
		long v = RecordGroup.longRecordGroup(conf, groups, 0);
		return v;
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	public BigInteger getBigIntRecordGroup(long loc) {
		int groupsLength = conf.recordGroupByteLength;
		if (groups == null || groups.length < groupsLength)
			ensureGroupsLength(groupsLength);
		// The condition avoids an unecessary synchronized method call
		getBytes(loc, groups, 0, groupsLength);
		BigInteger v = RecordGroup.bigIntRecordGroup(conf, groups, 0);
		return v;
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	public void putRecordGroup(long loc, long rg) {
		int groupsLength = conf.recordGroupByteLength;
		if (groups == null || groups.length < groupsLength)
			ensureGroupsLength(groupsLength);
		// The condition avoids an unecessary synchronized method call
		RecordGroup.toUnsignedByteArray(conf, rg, groups, 0);
		putBytes(loc, groups, 0, groupsLength);
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	public void putRecordGroup(long loc, BigInteger rg) {
		int groupsLength = conf.recordGroupByteLength;
		if (groups == null || groups.length < groupsLength)
			ensureGroupsLength(groupsLength);
		// The condition avoids an unecessary synchronized method call
		RecordGroup.toUnsignedByteArray(conf, rg, groups, 0);
		putBytes(loc, groups, 0, groupsLength);
	}

	/**
	 * Seek to this location and write len bytes from an array into the database
	 * 
	 * @param loc
	 *            The location to seek to
	 * @param arr
	 *            An array to read from
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to write
	 */
	public synchronized void putBytes(long loc, byte[] arr, int off, int len) {
		seek(loc);
		putBytes(arr, off, len);
	}

	/**
	 * Seek to this location and read len bytes from the database into an array
	 * 
	 * @param loc
	 *            The location to seek to
	 * @param arr
	 *            The array to write to
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to read
	 */
	public synchronized void getBytes(long loc, byte[] arr, int off, int len) {
		seek(loc);
		getBytes(arr, off, len);
	}

	/**
	 * Seek to this location in the database
	 * 
	 * @param loc
	 *            The location to seek to
	 */
	public abstract void seek(long loc);

	/**
	 * Writes len bytes from the array into the database
	 * 
	 * @param arr
	 *            An array to read from
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to write
	 */
	public abstract void putBytes(byte[] arr, int off, int len);

	/**
	 * Reads len bytes from the database into an array
	 * 
	 * @param arr
	 *            An array to write to
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to read
	 */
	public abstract void getBytes(byte[] arr, int off, int len);

	private final synchronized void ensureGroupsLength(int length) {
		if (groups == null || groups.length < length)
			groups = new byte[length];
	}

	/**
	 * Fills a portion of the database with the passed record.
	 * 
	 * @param r
	 *            The record
	 * @param offset
	 *            The byte offset into the database
	 * @param len
	 *            The number of bytes to fill
	 */
	public void fill(Record r, long offset, long len) {
		Record[] recs = new Record[conf.recordsPerGroup];
		for (int i = 0; i < conf.recordsPerGroup; i++)
			recs[i] = r;
		seek(offset);
		while (len > 0) {
			int groupsLength = (int) Math.min(len, maxBytes);
			int numGroups = groupsLength / conf.recordGroupByteLength;
			groupsLength = numGroups * conf.recordGroupByteLength;
			ensureGroupsLength(groupsLength);
			int onByte = 0;
			if (conf.recordGroupUsesLong) {
				long recordGroup = RecordGroup.longRecordGroup(conf, recs, 0);
				for (int i = 0; i < numGroups; i++) {
					RecordGroup.toUnsignedByteArray(conf, recordGroup, groups,
							onByte);
					onByte += conf.recordGroupByteLength;
				}
			} else {
				BigInteger recordGroup = RecordGroup.bigIntRecordGroup(conf,
						recs, 0);
				for (int i = 0; i < numGroups; i++) {
					RecordGroup.toUnsignedByteArray(conf, recordGroup, groups,
							onByte);
					onByte += conf.recordGroupByteLength;
				}

			}
			putBytes(groups, 0, groupsLength);
			len -= groupsLength;
		}
	}

	/**
	 * @return The number of bytes used to store all the records (This does not
	 *         include the header size)
	 */
	public long getByteSize() {
		return numBytes;
	}

	/**
	 * If this database only covers a particular range of hashes for a game,
	 * call this method before initialize if creating the database
	 * 
	 * @param firstByte
	 *            The first byte this database contains
	 * @param numBytes
	 *            The total number of bytes contained
	 */
	public void setRange(long firstByte, long numBytes) {
		this.firstByte = firstByte;
		this.numBytes = numBytes;
	}

	/**
	 * @return The index of the first byte in this database (Will be zero if
	 *         this database stores the entire game)
	 */
	public long firstByte() {
		return firstByte;
	}
}
