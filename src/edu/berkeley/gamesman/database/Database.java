package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
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

	private boolean solve;

	private long numBytes = -1;

	private long firstByte;

	private long position;

	private long location;

	protected DatabaseHandle myHandle;

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
	 * Return the Nth Record in the Database
	 * 
	 * @param recordIndex
	 *            The record number
	 * @return The stored Record
	 */
	public Record getRecord(long recordIndex) {
		Record r = conf.getGame().newRecord();
		getRecord(getHandle(), recordIndex, r);
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
	public void getRecord(DatabaseHandle dh, long recordIndex, Record r) {
		if (!solve)
			conf.getGame().setInterperet(recordIndex);
		if (conf.superCompress) {
			long group = recordIndex / conf.recordsPerGroup;
			int num = (int) (recordIndex % conf.recordsPerGroup);
			long byteOffset = group * conf.recordGroupByteLength;
			if (conf.recordGroupUsesLong)
				RecordGroup.getRecord(conf, getLongRecordGroup(dh, byteOffset),
						num, r);
			else
				RecordGroup.getRecord(conf,
						getBigIntRecordGroup(dh, byteOffset), num, r);
		} else
			RecordGroup.getRecord(conf, getLongRecordGroup(dh, recordIndex
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
	public void putRecord(DatabaseHandle dh, long recordIndex, Record r) {
		if (conf.superCompress) {
			int num = (int) (recordIndex % conf.recordsPerGroup);
			long byteOffset = recordIndex / conf.recordsPerGroup
					* conf.recordGroupByteLength;
			if (conf.recordGroupUsesLong) {
				long rg = getLongRecordGroup(dh, byteOffset);
				rg = RecordGroup.setRecord(conf, rg, num, r);
				putRecordGroup(dh, byteOffset, rg);
			} else {
				BigInteger rg = getBigIntRecordGroup(dh, byteOffset);
				rg = RecordGroup.setRecord(conf, rg, num, r);
				putRecordGroup(dh, byteOffset, rg);
			}
		} else
			putRecordGroup(dh, recordIndex * conf.recordGroupByteLength, r
					.getState());
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	public long getLongRecordGroup(DatabaseHandle dh, long loc) {
		byte[] groups = dh.getRecordGroupBytes(conf.recordGroupByteLength);
		getBytes(dh, loc, groups, 0, conf.recordGroupByteLength);
		long v = RecordGroup.longRecordGroup(conf, groups, 0);
		return v;
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	public BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		byte[] groups = dh.getRecordGroupBytes(conf.recordGroupByteLength);
		getBytes(dh, loc, groups, 0, conf.recordGroupByteLength);
		BigInteger v = RecordGroup.bigIntRecordGroup(conf, groups, 0);
		return v;
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	public void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		byte[] groups = dh.getRecordGroupBytes(conf.recordGroupByteLength);
		RecordGroup.toUnsignedByteArray(conf, rg, groups, 0);
		putBytes(dh, loc, groups, 0, conf.recordGroupByteLength);
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	public void putRecordGroup(DatabaseHandle dh, long loc, BigInteger rg) {
		byte[] groups = dh.getRecordGroupBytes(conf.recordGroupByteLength);
		RecordGroup.toUnsignedByteArray(conf, rg, groups, 0);
		putBytes(dh, loc, groups, 0, conf.recordGroupByteLength);
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
	public abstract void putBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len);

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
	public abstract void getBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len);

	/**
	 * Seek to this location in the database
	 * 
	 * @param loc
	 *            The location to seek to
	 */
	public synchronized void seek(long loc) {
		location = loc;
	}

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
	public synchronized void putBytes(byte[] arr, int off, int len) {
		if(myHandle==null)
			myHandle = getHandle();
		putBytes(myHandle, location, arr, off, len);
		location += len;
	}

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
	public synchronized void getBytes(byte[] arr, int off, int len) {
		if(myHandle==null)
			myHandle = getHandle();
		putBytes(myHandle, location, arr, off, len);
		location += len;
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
		int maxBytes = 1024 - 1024 % conf.recordGroupByteLength;
		byte[] groups = new byte[maxBytes];
		while (len > 0) {
			int groupsLength = (int) Math.min(len, maxBytes);
			int numGroups = groupsLength / conf.recordGroupByteLength;
			groupsLength = numGroups * conf.recordGroupByteLength;
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

	public DatabaseHandle getHandle() {
		return new DatabaseHandle();
	}

	public void closeHandle(DatabaseHandle dh) {
	}

	public DatabaseHandle getHandle(long recordStart, long numRecords) {
		return getHandle();
	}
}
