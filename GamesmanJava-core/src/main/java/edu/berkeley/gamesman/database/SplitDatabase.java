package edu.berkeley.gamesman.database;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import edu.berkeley.gamesman.core.Configuration;

/**
 * A database which points to a collection of other databases. If instantClose
 * is true, pointed databases will be lazily instantiated/opened and closed when
 * not in use. This means it's fine if the pointed database doesn't exist so
 * long as you don't try to read anything from it.
 * 
 * @author dnspies
 */
public abstract class SplitDatabase extends Database {
	/**
	 * Four features used to open an underlying database: class, uri,
	 * firstRecordIndex, and numRecords
	 * 
	 * @author dnspies
	 */
	public static class DatabaseDescriptor implements
			Comparable<DatabaseDescriptor> {
		/**
		 * @param dbClass
		 *            The class of the database
		 * @param uri
		 *            The path where the database file is
		 * @param firstRecordIndex
		 *            The index of the first record contained in the database
		 * @param numRecords
		 *            The number of records in the database
		 */
		public DatabaseDescriptor(String dbClass, String uri,
				long firstRecordIndex, long numRecords) {
			this.dbClass = dbClass;
			this.uri = uri;
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
		}

		/**
		 * The class of the database
		 */
		public final String dbClass;
		/**
		 * The path where the database file is
		 */
		public final String uri;
		/**
		 * The index of the first record contained in the database
		 */
		public final long firstRecordIndex;
		/**
		 * The number of records in the database
		 */
		public final long numRecords;

		@Override
		public int compareTo(DatabaseDescriptor o) {
			if (this.firstRecordIndex < o.firstRecordIndex)
				return -1;
			else if (this.firstRecordIndex > o.firstRecordIndex)
				return 1;
			else if (this.numRecords < o.numRecords)
				return -1;
			else if (this.numRecords > o.numRecords)
				return 1;
			else
				return 0;
		}
	}

	private final DatabaseDescriptor[] dd;
	private final Database[] containedDbs;
	private final int[] using;
	private final long[] rangeByteIndexStarts;
	private final boolean instantClose;
	private boolean holding = false;

	/**
	 * @param dis
	 *            An input stream to read in the descriptors
	 * @param conf
	 *            The configuration object
	 * @param firstRecordIndex
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The number of records contained in this database
	 * @param reading
	 *            Whether to open the database for reading
	 * @param writing
	 *            Whether to open the database for writing
	 * @throws IOException
	 *             If an IOException occurs while reading the header
	 * @throws ClassNotFoundException
	 *             If a ClassNotFoundException occurs while reading the
	 *             configuration for any underlying database
	 */
	public SplitDatabase(DataInputStream dis, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, ClassNotFoundException {
		this(dis, conf, firstRecordIndex, numRecords, reading, writing, false);
	}

	/**
	 * @param dis
	 *            An input stream to read in the descriptors
	 * @param conf
	 *            The configuration object
	 * @param firstRecordIndex
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The number of records contained in this database
	 * @param reading
	 *            Whether to open the database for reading
	 * @param writing
	 *            Whether to open the database for writing
	 * @param instantClose
	 *            If the underlying databases should be opened/instantiated
	 *            lazily and closed when not needed
	 * @throws IOException
	 *             If an IOException occurs while reading the header
	 * @throws ClassNotFoundException
	 *             If a ClassNotFoundException occurs while reading the
	 *             configuration for any underlying database
	 */
	public SplitDatabase(DataInputStream dis, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing, boolean instantClose) throws IOException,
			ClassNotFoundException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		this.instantClose = instantClose;
		assert !(instantClose && writing);
		skipHeader(dis);
		LinkedList<DatabaseDescriptor> dbList = new LinkedList<DatabaseDescriptor>();
		long lastRecordIndex = firstRecordIndex + numRecords;
		DatabaseDescriptor lastDescriptor = null;
		long lastLastRecordIndex = 0L;
		while (true) {
			DatabaseDescriptor nextDescriptor;
			try {
				nextDescriptor = new DatabaseDescriptor(dis.readUTF(),
						dis.readUTF(), dis.readLong(), dis.readLong());
			} catch (EOFException e) {
				break;
			}
			assert lastDescriptor == null
					|| nextDescriptor.firstRecordIndex == lastLastRecordIndex;
			long nextLastRecordIndex = nextDescriptor.firstRecordIndex
					+ nextDescriptor.numRecords;
			if (nextDescriptor.firstRecordIndex < lastRecordIndex
					&& nextLastRecordIndex > firstRecordIndex)
				dbList.add(nextDescriptor);
			lastDescriptor = nextDescriptor;
			lastLastRecordIndex = nextLastRecordIndex;
		}
		dis.close();
		containedDbs = new Database[dbList.size()];
		dd = dbList.toArray(new DatabaseDescriptor[dbList.size()]);
		if (instantClose)
			using = new int[dbList.size()];
		else
			using = null;
		rangeByteIndexStarts = new long[dbList.size() + 1];
		int i = 0;
		for (DatabaseDescriptor db : dbList) {
			if (!instantClose)
				addDb(i, db);
			rangeByteIndexStarts[i] = myLogic.getByteIndex(db.firstRecordIndex);
			i++;
		}
		rangeByteIndexStarts[i] = myLogic.getByteIndex(lastRecordIndex);
	}

	private Database newDatabase(DatabaseDescriptor db) throws IOException,
			ClassNotFoundException {
		return Database.openDatabase(db.dbClass, db.uri, conf,
				db.firstRecordIndex, db.numRecords, reading, writing);
	}

	private class SplitHandle extends DatabaseHandle {
		private final DatabaseHandle[] innerHandles;
		private int currentDb = -1;

		public SplitHandle(int numBytes, boolean reading) {
			super(numBytes, reading);
			innerHandles = new DatabaseHandle[containedDbs.length];
		}

	}

	@Override
	public DatabaseHandle getHandle(boolean reading) {
		assert reading ? this.reading : this.writing;
		return new SplitHandle(myLogic.recordBytes, reading);
	}

	@Override
	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		long endByteIndex = firstByteIndex + numBytes;
		int firstDb = findDb(firstByteIndex);
		int lastDb = findDb(endByteIndex - 1);
		SplitHandle sh = (SplitHandle) dh;
		for (int dbNum = firstDb; dbNum <= lastDb; dbNum++) {
			if (instantClose) {
				try {
					incrementDb(dbNum);
				} catch (ClassNotFoundException e) {
					throw new Error(e);
				}
			}
			DatabaseHandle dh2 = getInnerHandle(sh, dbNum);
			long innerFirst = dbNum == firstDb ? firstByteIndex
					: rangeByteIndexStarts[dbNum];
			long innerNum = (dbNum == lastDb ? endByteIndex
					: rangeByteIndexStarts[dbNum + 1]) - innerFirst;
			containedDbs[dbNum].prepareReadRange(dh2, innerFirst, innerNum);
		}
		sh.currentDb = firstDb;
	}

	private synchronized void incrementDb(int dbNum) throws IOException,
			ClassNotFoundException {
		if (containedDbs[dbNum] == null) {
			addDb(dbNum, dd[dbNum]);
		}
		using[dbNum]++;
	}

	private synchronized void decrementDb(int dbNum) throws IOException {
		using[dbNum]--;
		if (using[dbNum] == 0 && !holding) {
			containedDbs[dbNum].close();
			containedDbs[dbNum] = null;
		}
	}

	private synchronized void addDb(int dbNum, DatabaseDescriptor dd)
			throws IOException, ClassNotFoundException {
		containedDbs[dbNum] = newDatabase(dd);
		if (instantClose && containedDbs[dbNum] instanceof SplitDatabase) {
			((SplitDatabase) containedDbs[dbNum]).setHolding(true);
		}
	}

	private DatabaseHandle getInnerHandle(SplitHandle sh, int dbNum) {
		DatabaseHandle dh2 = sh.innerHandles[dbNum];
		if (dh2 == null) {
			dh2 = containedDbs[dbNum].getHandle(sh.reading);
			sh.innerHandles[dbNum] = dh2;
		}
		return dh2;
	}

	private int findDb(long byteIndex) {
		int db = Arrays.binarySearch(rangeByteIndexStarts, byteIndex);
		if (db < 0) {
			db = -db - 2;
		}
		return db;
	}

	@Override
	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		long endByteIndex = firstByteIndex + numBytes;
		int firstDb = findDb(firstByteIndex);
		int lastDb = findDb(endByteIndex - 1);
		SplitHandle sh = (SplitHandle) dh;
		for (int dbNum = firstDb; dbNum <= lastDb; dbNum++) {
			DatabaseHandle dh2 = getInnerHandle(sh, dbNum);
			long innerFirst = dbNum == firstDb ? firstByteIndex
					: rangeByteIndexStarts[dbNum];
			long innerNum = (dbNum == lastDb ? endByteIndex
					: rangeByteIndexStarts[dbNum + 1]) - innerFirst;
			containedDbs[dbNum].prepareWriteRange(dh2, innerFirst, innerNum);
		}
		sh.currentDb = firstDb;
	}

	@Override
	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		SplitHandle sh = (SplitHandle) dh;
		long nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		while (sh.location == nextStop) {
			sh.currentDb++;
			nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		}
		int toRead = (int) Math.min(maxLen, nextStop - sh.location);
		int bytesRead = containedDbs[sh.currentDb].readBytes(
				sh.innerHandles[sh.currentDb], array, off, toRead);
		if (instantClose && dh.remainingBytes == bytesRead) {
			closeDbs(sh);
		}
		return bytesRead;
	}

	private void closeDbs(SplitHandle dh) throws IOException {
		long endByteIndex = dh.firstByteIndex + dh.numBytes;
		int firstDb = findDb(dh.firstByteIndex);
		int lastDb = findDb(endByteIndex - 1);
		for (int dbNum = firstDb; dbNum <= lastDb; dbNum++) {
			decrementDb(dbNum);
			dh.innerHandles[dbNum] = null;
		}
	}

	@Override
	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		SplitHandle sh = (SplitHandle) dh;
		long nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		while (sh.location == nextStop) {
			sh.currentDb++;
			nextStop = rangeByteIndexStarts[sh.currentDb + 1];
		}
		int toWrite = (int) Math.min(maxLen, nextStop - sh.location);
		return containedDbs[sh.currentDb].writeBytes(
				sh.innerHandles[sh.currentDb], array, off, toWrite);
	}

	@Override
	public void close() throws IOException {
		setHolding(false);
		for (Database db : containedDbs) {
			if (instantClose)
				assert db == null;
			else
				db.close();
		}
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		throw new UnsupportedOperationException();
	}

	/**
	 * When set to true, tells underlying databases to remain open whether or
	 * not they're still in use. These databases will be closed when
	 * setHolding(false) is called
	 * 
	 * @param holding
	 *            true to hold databases open, false to close them
	 * @return true if holding has changed from it's previous value
	 * @throws IOException
	 *             If an IOException occurs while closing databases
	 */
	public boolean setHolding(boolean holding) throws IOException {
		if (holding == this.holding || !instantClose)
			return false;
		this.holding = holding;
		if (!holding) {
			for (int i = 0; i < containedDbs.length; i++) {
				if (containedDbs[i] != null && using[i] == 0) {
					containedDbs[i].close();
					containedDbs[i] = null;
				}
			}
		}
		return true;
	}
}
