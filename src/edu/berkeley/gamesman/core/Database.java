package edu.berkeley.gamesman.core;

import java.util.Iterator;

import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * A Database is the abstract superclass of all data storage methods used in
 * Gamesman. Each particular Database is responsible for the persistent storage
 * of Records derived from solving games.
 * 
 * @author Steven Schlansker
 */
public abstract class Database {

	protected Configuration conf;

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
	 */
	public final void initialize(String uri, Configuration config) {
		conf = config;
		initialize(uri);
	}

	/**
	 * Initializes as above, but when the confiration is already specified
	 * 
	 * @param uri
	 *            The URI that the Database is associated with
	 */
	public abstract void initialize(String uri);

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
	 * Return the Nth Record in the Database
	 * 
	 * @param recordIndex
	 *            The record number
	 * @return The stored Record
	 */
	public Record getRecord(long recordIndex) {
		long group = recordIndex / conf.recordsPerGroup;
		int num = (int) (recordIndex % conf.recordsPerGroup);
		long byteOffset = group * conf.recordGroupByteLength;
		if (conf.recordGroupUsesLong)
			return RecordGroup.getRecord(conf, getLongRecordGroup(byteOffset),
					num);
		else
			return RecordGroup.getRecord(conf,
					getBigIntRecordGroup(byteOffset), num);
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
		long group = recordIndex / conf.recordsPerGroup;
		int num = (int) (recordIndex % conf.recordsPerGroup);
		long byteOffset = group * conf.recordGroupByteLength;
		if (conf.recordGroupUsesLong)
			RecordGroup.getRecord(conf, getLongRecordGroup(byteOffset), num, r);
		else
			RecordGroup.getRecord(conf, getBigIntRecordGroup(byteOffset), num,
					r);
	}

	/**
	 * @param recordIndex
	 *            The index to look in
	 * @param numRecords
	 *            The number of records before hasNext returns false
	 * @return An iterator starting at recordIndex over numRecords records in
	 *         this database
	 */
	public Iterator<Record> getRecords(long recordIndex, int numRecords) {
		long byteOffset = recordIndex / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		int preRecords = (int) (recordIndex % conf.recordsPerGroup);
		int recordGroups = (numRecords + preRecords - 1) / conf.recordsPerGroup
				+ 1;
		RecordIterator ri;
		if (conf.recordGroupUsesLong)
			ri = new RecordIterator(getLongRecordGroups(byteOffset,
					recordGroups), preRecords, numRecords);
		else
			ri = new RecordIterator(getBigIntRecordGroups(byteOffset,
					recordGroups), preRecords, numRecords);
		return ri;
	}

	/**
	 * Store a record in the Database
	 * 
	 * @param recordIndex
	 *            The record number
	 * @param r
	 *            The Record to store
	 */
	public void putRecord(long recordIndex, Record r) {
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
	}

	/**
	 * Stores numRecords records from the records iterator in the database
	 * 
	 * @param recordIndex
	 *            The index to look in
	 * @param records
	 *            An iterator starting at recordIndex over numRecords records
	 * @param numRecords
	 *            The number of records to go through
	 */
	public synchronized void putRecords(long recordIndex,
			RecordIterator records, int numRecords) {
		int preRecords = conf.recordsPerGroup
				- ((int) ((recordIndex - 1) % conf.recordsPerGroup) + 1);
		int recordGroups = (numRecords - preRecords) / conf.recordsPerGroup;
		int mainRecords = recordGroups * conf.recordsPerGroup;
		int postRecords = (numRecords - preRecords) % conf.recordsPerGroup;
		for (int i = 0; i < preRecords; i++) {
			putRecord(recordIndex++, records.next());
		}
		if (conf.recordGroupUsesLong) {
			LongRecordGroupIterator rgi = new LongRecordGroupIterator(records);
			long groupByteOffset = recordIndex / conf.recordsPerGroup
					* conf.recordGroupByteLength;
			putRecordGroups(groupByteOffset, rgi, recordGroups);
		} else {
			BigIntRecordGroupIterator rgi = new BigIntRecordGroupIterator(
					records);
			long groupByteOffset = recordIndex / conf.recordsPerGroup
					* conf.recordGroupByteLength;
			putRecordGroups(groupByteOffset, rgi, recordGroups);
		}
		recordIndex += mainRecords;
		for (int i = 0; i < postRecords; i++) {
			putRecord(recordIndex++, records.next());
		}
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	public abstract long getLongRecordGroup(long loc);

	public abstract BigInteger getBigIntRecordGroup(long loc);

	/**
	 * @param startLoc
	 *            The location to start at
	 * @param numGroups
	 *            The number of groups to return
	 * @return An iterator over numGroups RecordGroups from this database
	 */
	public Iterator<BigInteger> getBigIntRecordGroups(long startLoc,
			int numGroups) {
		throw new UnsupportedOperationException(
				"getRecordGroups should be overridden");
	}

	/**
	 * @param startLoc
	 *            The location to start at
	 * @param numGroups
	 *            The number of groups to return
	 * @return An iterator over numGroups RecordGroups from this database
	 */
	public LongIterator getLongRecordGroups(long startLoc, int numGroups) {
		throw new UnsupportedOperationException(
				"getRecordGroups should be overridden");
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	public abstract void putRecordGroup(long loc, long rg);

	public abstract void putRecordGroup(long loc, BigInteger rg);

	/**
	 * Puts numGroups RecordGroups into this database starting at location loc
	 * (loc is measured in bytes).
	 * 
	 * @param loc
	 *            The location to start at
	 * @param it
	 *            An iterator over at least numGroups RecordGroups
	 * @param numGroups
	 *            The number of groups to store
	 */
	public void putRecordGroups(long loc, LongIterator it, int numGroups) {
		throw new UnsupportedOperationException(
				"putRecordGroups should be overridden");
	}

	/**
	 * Puts numGroups RecordGroups into this database starting at location loc
	 * (loc is measured in bytes).
	 * 
	 * @param loc
	 *            The location to start at
	 * @param it
	 *            An iterator over at least numGroups RecordGroups
	 * @param numGroups
	 *            The number of groups to store
	 */
	public void putRecordGroups(long loc, Iterator<BigInteger> it, int numGroups) {
		throw new UnsupportedOperationException(
				"putRecordGroups should be overridden");
	}

	/**
	 * @return The number of bytes used to store all the records (This does not
	 *         include the header size)
	 */
	public long getByteSize() {
		return (conf.getGame().lastHash() / conf.recordsPerGroup + 1)
				* conf.recordGroupByteLength;
	}

	private class LongRecordGroupIterator implements LongIterator {
		private Iterator<Record> recordIterator;

		private Record[] records;

		private int stop;

		private int index;

		private LongRecordGroupIterator(Iterator<Record> recordIterator) {
			this.recordIterator = recordIterator;
		}

		public LongRecordGroupIterator(Record[] records, int offset, int length) {
			this.records = records;
			this.stop = offset + length;
			this.index = offset;
		}

		public boolean hasNext() {
			if (recordIterator == null)
				return index < stop;
			else
				return recordIterator.hasNext();
		}

		public long next() {
			if (recordIterator == null) {
				long rg = RecordGroup.longRecordGroup(conf, records, index);
				index += conf.recordsPerGroup;
				return rg;
			} else
				return RecordGroup.longRecordGroup(conf, recordIterator);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported");
		}
	}

	private class BigIntRecordGroupIterator implements Iterator<BigInteger> {
		private Iterator<Record> recordIterator;

		private Record[] records;

		private int stop;

		private int index;

		private BigIntRecordGroupIterator(Iterator<Record> recordIterator) {
			this.recordIterator = recordIterator;
		}

		public BigIntRecordGroupIterator(Record[] records, int offset,
				int length) {
			this.records = records;
			this.stop = offset + length;
			this.index = offset;
		}

		public boolean hasNext() {
			if (recordIterator == null)
				return index < stop;
			else
				return recordIterator.hasNext();
		}

		public BigInteger next() {
			if (recordIterator == null) {
				BigInteger rg = RecordGroup.bigIntRecordGroup(conf, records,
						index);
				index += conf.recordsPerGroup;
				return rg;
			} else
				return RecordGroup.bigIntRecordGroup(conf, recordIterator);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported");
		}
	}

	private class RecordIterator implements Iterator<Record> {
		private final Record[] currentRecords;

		private long nextRecord = 0;

		private final long numRecords;

		int index;

		private LongIterator longRecordGroups;

		private Iterator<BigInteger> recordGroups;

		private RecordIterator(Iterator<BigInteger> recordGroups,
				int preRecords, long numRecords) {
			this.recordGroups = recordGroups;
			this.numRecords = numRecords + preRecords;
			currentRecords = new Record[conf.recordsPerGroup];
			for (int i = 0; i < conf.recordsPerGroup; i++)
				currentRecords[i] = conf.getGame().newRecord();
			index = conf.recordsPerGroup;
			for (int i = 0; i < preRecords; i++)
				next();
		}

		private RecordIterator(LongIterator recordGroups, int preRecords,
				long numRecords) {
			this.longRecordGroups = recordGroups;
			this.numRecords = numRecords + preRecords;
			currentRecords = new Record[conf.recordsPerGroup];
			for (int i = 0; i < conf.recordsPerGroup; i++)
				currentRecords[i] = conf.getGame().newRecord();
			index = conf.recordsPerGroup;
			for (int i = 0; i < preRecords; i++)
				next();
		}

		public boolean hasNext() {
			return nextRecord < numRecords;
		}

		public Record next() {
			if (index >= conf.recordsPerGroup) {
				if (conf.recordGroupUsesLong)
					RecordGroup.getRecords(conf, longRecordGroups.next(),
							currentRecords, 0);
				else
					RecordGroup.getRecords(conf, recordGroups.next(),
							currentRecords, 0);
				index = 0;
			}
			return currentRecords[index++].clone();
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported");
		}

	}

	/**
	 * Stores numRecords records from the records array in the database
	 * 
	 * @param recordIndex
	 *            The index to look in
	 * @param records
	 *            An array containing records
	 * @param offset
	 *            The offset at which to start reading from the array
	 * @param numRecords
	 *            The number of records to go through
	 */
	public synchronized void putRecords(long recordIndex, Record[] records,
			int offset, int numRecords) {
		int preRecords = conf.recordsPerGroup
				- ((int) ((recordIndex - 1) % conf.recordsPerGroup) + 1);
		int recordGroups = (numRecords - preRecords) / conf.recordsPerGroup;
		int mainRecords = recordGroups * conf.recordsPerGroup;
		int postRecords = (numRecords - preRecords) % conf.recordsPerGroup;
		for (int i = 0; i < preRecords; i++) {
			putRecord(recordIndex++, records[offset++]);
		}
		long groupByteOffset = recordIndex / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		if (conf.recordGroupUsesLong) {
			LongRecordGroupIterator rgi = new LongRecordGroupIterator(records,
					offset, numRecords);
			putRecordGroups(groupByteOffset, rgi, recordGroups);
		} else {
			BigIntRecordGroupIterator rgi = new BigIntRecordGroupIterator(
					records, offset, numRecords);
			putRecordGroups(groupByteOffset, rgi, recordGroups);
		}
		recordIndex += mainRecords;
		offset += mainRecords;
		for (int i = 0; i < postRecords; i++) {
			putRecord(recordIndex++, records[offset++]);
		}
	}
}
