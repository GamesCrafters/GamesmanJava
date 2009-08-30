package edu.berkeley.gamesman.core;

import java.util.Iterator;

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
	public synchronized Record getRecord(long recordIndex) {
		long group = recordIndex / conf.recordsPerGroup;
		int num = (int) (recordIndex % conf.recordsPerGroup);
		long byteOffset = group * conf.recordGroupByteLength;
		return getRecordGroup(byteOffset).getRecord(num);
	}

	public Iterator<Record> getRecords(long recordIndex, int numRecords) {
		int num = (int) (recordIndex % conf.recordsPerGroup);
		long byteOffset = recordIndex / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		int preRecords = (int) (recordIndex % conf.recordsPerGroup);
		int recordGroups = (numRecords + preRecords - 1) / conf.recordsPerGroup
				+ 1;
		RecordIterator ri = new RecordIterator(getRecordGroups(byteOffset,
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
	public synchronized void putRecord(long recordIndex, Record r) {
		int num = (int) (recordIndex % conf.recordsPerGroup);
		long byteOffset = recordIndex / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		RecordGroup rg = getRecordGroup(byteOffset);
		rg.setRecord(num, r);
		putRecordGroup(byteOffset, rg);
	}

	public void putRecords(long recordIndex, Iterator<Record> r, int numRecords) {
		int preRecords = (int) (conf.recordsPerGroup - recordIndex
				% conf.recordsPerGroup);
		int recordGroups = (numRecords - preRecords) / conf.recordsPerGroup;
		int postRecords = (numRecords - preRecords) % conf.recordsPerGroup;
		for (long i = 0; i < preRecords; i++) {
			putRecord(recordIndex++, r.next());
		}
		RecordGroupIterator rgi = new RecordGroupIterator(r);
		long groupByteOffset = recordIndex / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		putRecordGroups(groupByteOffset, rgi, recordGroups);
		recordIndex += recordGroups * conf.recordsPerGroup;
		for (long i = 0; i < postRecords; i++) {
			putRecord(recordIndex++, r.next());
		}
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	public abstract RecordGroup getRecordGroup(long loc);

	public Iterator<RecordGroup> getRecordGroups(long startLoc, int numGroups) {
		throw new UnsupportedOperationException(
				"getRecordGroups should be overridden");
	}

	/**
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	public abstract void putRecordGroup(long loc, RecordGroup rg);

	public void putRecordGroups(long loc, Iterator<RecordGroup> it,
			int numGroups) {
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

	private class RecordGroupIterator implements Iterator<RecordGroup> {
		private Iterator<Record> recordIterator;

		Record[] records;

		int offset, stop;

		int index;

		private RecordGroupIterator(Iterator<Record> recordIterator) {
			this.recordIterator = recordIterator;
		}

		public RecordGroupIterator(Record[] records, int offset, int length) {
			this.records = records;
			this.offset = offset;
			this.stop = offset + length;
			this.index = offset;
		}

		public boolean hasNext() {
			if (recordIterator == null)
				return index < stop;
			else
				return recordIterator.hasNext();
		}

		public RecordGroup next() {
			BigInteger bi = BigInteger.ZERO;
			if (recordIterator == null) {
				for (int i = 0; i < conf.recordsPerGroup; i++) {
					bi = bi.multiply(conf.totalStates);
					if (index < stop)
						bi = bi.add(records[index++].getState());
				}
			} else {
				for (int i = 0; i < conf.recordsPerGroup; i++) {
					bi = bi.multiply(conf.totalStates);
					if (recordIterator.hasNext())
						bi = bi.add(recordIterator.next().getState());
				}
			}
			return new RecordGroup(conf, bi);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported");
		}
	}

	private class RecordIterator implements Iterator<Record> {
		private BigInteger currentGroup;

		private long nextRecord = 0;

		private final long numRecords;

		private final BigInteger startDivide = conf.totalStates
				.pow(conf.recordsPerGroup - 1);

		private BigInteger divide = BigInteger.ZERO;

		private Iterator<RecordGroup> recordGroups;

		private RecordIterator(Iterator<RecordGroup> recordGroups,
				int preRecords, long numRecords) {
			this.recordGroups = recordGroups;
			this.numRecords = numRecords + preRecords;
			for (int i = 0; i < preRecords; i++)
				next();
		}

		public boolean hasNext() {
			return nextRecord < numRecords;
		}

		public Record next() {
			if (divide.equals(BigInteger.ZERO)) {
				divide = startDivide;
				currentGroup = recordGroups.next().getState();
			}
			BigInteger ret = currentGroup.divide(divide);
			currentGroup = currentGroup.subtract(ret.multiply(divide));
			divide = divide.divide(conf.totalStates);
			nextRecord++;
			return new Record(conf, ret);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported");
		}

	}

	public void putRecords(long recordIndex, Record[] records, int offset,
			int numRecords) {
		int preRecords = (int) (conf.recordsPerGroup - recordIndex
				% conf.recordsPerGroup);
		int recordGroups = (numRecords - preRecords) / conf.recordsPerGroup;
		int mainRecords = recordGroups * conf.recordsPerGroup;
		int postRecords = (numRecords - preRecords) % conf.recordsPerGroup;
		for (int i = 0; i < preRecords; i++) {
			putRecord(recordIndex++, records[offset++]);
		}
		RecordGroupIterator rgi = new RecordGroupIterator(records, offset,
				numRecords);
		long groupByteOffset = recordIndex / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		putRecordGroups(groupByteOffset, rgi, recordGroups);
		recordIndex += mainRecords;
		offset += mainRecords;
		for (long i = 0; i < postRecords; i++) {
			putRecord(recordIndex++, records[offset++]);
		}
	}
}
