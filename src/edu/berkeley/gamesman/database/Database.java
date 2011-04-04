package edu.berkeley.gamesman.database;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.wrapper.DatabaseWrapper;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Util;

/**
 * @author dnspies
 */
public abstract class Database implements Flushable, Closeable {

	/**
	 * The configuration object corresponding to this database (may have been
	 * loaded from the database file or used to create the database object)
	 */
	public final Configuration conf;

	/**
	 * Whether or not reading is enabled for this database
	 */
	public final boolean reading;

	/**
	 * Whether or not writing is enabled for this database. In some cases, a
	 * database can only be either reading or writing, but not both. Then
	 * writing = true takes precedence over reading = true
	 */
	public final boolean writing;

	/**
	 * The database logic used to convert between record indices and byte
	 * indices
	 */
	public final DatabaseLogic myLogic;

	/**
	 * The index of the first record contained in this database
	 */
	public final long firstRecordIndex;

	/**
	 * The total number of records contained in this database
	 */
	public final long numRecords;

	/**
	 * Note: If both writing and reading are enabled, it is still generally
	 * assumed that you will be ignoring and/or overwriting any database file
	 * which may have previously been stored in its place. To read from an
	 * existing file, the database must be initialized as read only (or else the
	 * child class must have a specialized constructor)
	 * 
	 * @param conf
	 *            The configuration object corresponding to this database
	 * @param firstRecordIndex
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The total number of records contained in this database
	 * @param reading
	 *            Whether or not reading is enabled for this database
	 * @param writing
	 *            Whether or not writing is enabled for this database.
	 */
	public Database(Configuration conf, long firstRecordIndex, long numRecords,
			boolean reading, boolean writing) {
		this.conf = conf;
		this.reading = reading;
		this.writing = writing;
		assert reading || writing;
		long recordStates = conf.getGame().recordStates();
		myLogic = new DatabaseLogic(recordStates);
		this.firstRecordIndex = firstRecordIndex;
		this.numRecords = numRecords;
	}

	/**
	 * DatabaseHandles are necessary to perform all major database operations. A
	 * DatabaseHandle contains information which must be duplicated for each
	 * thread intending to access the database (so don't share handles across
	 * threads!)
	 * 
	 * @param reading
	 *            true = This handle is for reading, false = This handle is for
	 *            writing
	 * @return A handle to be used to access this database
	 */
	public DatabaseHandle getHandle(boolean reading) {
		assert reading ? this.reading : this.writing;
		return new DatabaseHandle(myLogic.recordBytes, reading);
	}

	/**
	 * @return The index of the first byte contained in this database as a
	 *         function of the index of the first record
	 */
	protected final long firstByteIndex() {
		return myLogic.getByteIndex(firstRecordIndex);
	}

	/**
	 * @return The number of bytes contained in this database as a function of
	 *         the number of records
	 */
	protected final long numBytes() {
		return myLogic.getNumBytes(numRecords);
	}

	/**
	 * Prepares a database handle for reading a particular range of bytes. You
	 * should make sure to read the entire range as system resources may be left
	 * open if you stop in the middle or fail to finish.
	 * 
	 * @param dh
	 *            The handle which will be used for reading
	 * @param firstByteIndex
	 *            The index of the first byte to be read
	 * @param numBytes
	 *            The number of bytes to be read
	 * @throws IOException
	 *             If an IOException occurs during preparation
	 */
	public final void prepareReadRange(DatabaseHandle dh, long firstByteIndex,
			long numBytes) throws IOException {
		assert reading;
		assert dh.remainingBytes == 0;
		dh.location = dh.firstByteIndex = firstByteIndex;
		dh.numBytes = numBytes;
		if (numBytes == DatabaseHandle.KEEP_GOING)
			dh.remainingBytes = 0;
		else
			dh.remainingBytes = numBytes;
		lowerPrepareReadRange(dh, firstByteIndex, numBytes);
	}

	/**
	 * Subclasses may override this method to add additional instructions when
	 * preparing to read a range of records. This method should not be called
	 * directly. Call prepareReadRange instead.
	 * 
	 * @see #prepareReadRange
	 * 
	 * @param dh
	 *            The handle which will be used for reading
	 * @param firstByteIndex
	 *            The index of the first byte to be read
	 * @param numBytes
	 *            The number off bytes to be read
	 * @throws IOException
	 *             If an IOException occurs during preparation
	 */
	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
	}

	/**
	 * Prepares a database handle for writing a particular range of bytes. You
	 * should make sure to write the entire range as system resources may be
	 * left open (or bytes may be left in a buffer) if you stop in the middle or
	 * fail to finish.
	 * 
	 * @param dh
	 *            The handle which will be used for writing
	 * @param firstByteIndex
	 *            The index of the first byte to be written
	 * @param numBytes
	 *            The number of bytes to be written
	 * @throws IOException
	 *             If an IOException occurs during preparation
	 */
	public final void prepareWriteRange(DatabaseHandle dh, long firstByteIndex,
			long numBytes) throws IOException {
		assert writing;
		assert dh.remainingBytes == 0;
		dh.location = dh.firstByteIndex = firstByteIndex;
		dh.numBytes = numBytes;
		if (numBytes == -1)
			dh.remainingBytes = 0;
		else
			dh.remainingBytes = numBytes;
		lowerPrepareWriteRange(dh, firstByteIndex, numBytes);
	}

	/**
	 * Subclasses may override this method to add additional instructions when
	 * preparing to write a range of records. This method should not be called
	 * directly. Call prepareWriteRange instead.
	 * 
	 * @see #prepareWriteRange
	 * 
	 * @param dh
	 *            The handle which will be used for writing
	 * @param firstByteIndex
	 *            The index of the first byte to be written
	 * @param numBytes
	 *            The number of bytes to be written
	 * @throws IOException
	 *             If an IOException occurs during preparation
	 */
	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
	}

	/**
	 * Reads a range of bytes from the database into the array. prepareReadRange
	 * must have been called on the handle first so it knows where to start
	 * reading from.
	 * 
	 * @param dh
	 *            The handle to use for reading
	 * @param array
	 *            The array to read into
	 * @param off
	 *            The offset into the array to start at
	 * @param maxLen
	 *            The maximum number of bytes to read (fewer bytes may be read
	 *            if the end of the prepared range is reached or for other
	 *            reasons)
	 * @return The number of bytes actually read
	 * @throws IOException
	 *             If an IOException occurs while reading
	 */
	protected final int readBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		int actualNum;
		if (dh.numBytes == DatabaseHandle.UNPREPARED) {
			throw new UnpreparedHandleException(dh);
		} else if (dh.numBytes == DatabaseHandle.KEEP_GOING)
			actualNum = maxLen;
		else if (dh.numBytes == 0)
			return -1;
		else
			actualNum = (int) Math.min(dh.remainingBytes, maxLen);
		int read = lowerReadBytes(dh, array, off, actualNum);
		if (read < 0)
			throw new EOFException();
		dh.location += read;
		if (dh.numBytes >= 0)
			dh.remainingBytes -= read;
		return read;
	}

	/**
	 * Subclasses may override this method to specify how to read an already
	 * prepared range of bytes. If so, the other readBytes which takes in a
	 * location can just be implemented to throw an
	 * UnsupportedOperationException
	 * 
	 * @see #readBytes(DatabaseHandle, long, byte[], int, int)
	 * 
	 * @param dh
	 *            The handle to use for reading
	 * @param array
	 *            The array to read into
	 * @param off
	 *            The offset into the array to start at
	 * @param len
	 *            The maximum number of bytes to read (fewer bytes may be read
	 *            if the end of the prepared range is reached or for other
	 *            reasons)
	 * @return The number of bytes actually read
	 * @throws IOException
	 *             If an IOException occurs while reading
	 */
	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return readBytes(dh, dh.location, array, off, len);
	}

	/**
	 * This method reads a range of bytes from the database into an array. This
	 * range need not necessarily have been prepared already (although the
	 * default implementation of lowerReadBytes calls this method, so it may
	 * have)
	 * 
	 * @param dh
	 *            The handle to use for reading
	 * @param location
	 *            The location in the database to start reading from
	 * @param array
	 *            The array to read to
	 * @param off
	 *            The index into the array to start at
	 * @param len
	 *            The maximum number of bytes to read (fewer bytes may be read)
	 * @return The number of bytes actually read
	 * @throws IOException
	 *             If an IOException occurs while reading
	 */
	protected abstract int readBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException;

	protected final int writeBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		int actualNum;
		if (dh.numBytes == DatabaseHandle.UNPREPARED)
			throw new UnpreparedHandleException(dh);
		else if (dh.numBytes == DatabaseHandle.KEEP_GOING)
			actualNum = maxLen;
		else if (dh.numBytes == 0)
			return -1;
		else
			actualNum = (int) Math.min(dh.remainingBytes, maxLen);
		int written = lowerWriteBytes(dh, array, off, actualNum);
		if (written < 0)
			throw new EOFException();
		dh.location += written;
		if (dh.numBytes >= 0)
			dh.remainingBytes -= written;
		return written;
	}

	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return writeBytes(dh, dh.location, array, off, len);
	}

	protected abstract int writeBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException;

	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		return readRecordFromByteIndex(dh, myLogic.getByteIndex(recordIndex));
	}

	protected long readRecordFromByteIndex(DatabaseHandle dh, long byteIndex)
			throws IOException {
		readFullBytes(dh, byteIndex, dh.currentRecord, 0, myLogic.recordBytes);
		return myLogic.getRecord(dh.currentRecord, 0);
	}

	protected final void readFullBytes(DatabaseHandle dh, long byteIndex,
			byte[] recordArray, int off, int len) throws IOException {
		prepareReadRange(dh, byteIndex, len);
		readFullBytes(dh, recordArray, off, len);
	}

	public final void readFullBytes(DatabaseHandle dh, byte[] byteArray,
			int off, int len) throws IOException {
		while (len > 0) {
			int read = readBytes(dh, byteArray, off, len);
			if (read < 0)
				throw new EOFException();
			off += read;
			len -= read;
		}
	}

	public void writeRecord(DatabaseHandle dh, long recordIndex, long record)
			throws IOException {
		writeRecordFromByteIndex(dh, myLogic.getByteIndex(recordIndex), record);
	}

	protected void writeRecordFromByteIndex(DatabaseHandle dh, long byteIndex,
			long record) throws IOException {
		myLogic.fillBytes(record, dh.currentRecord, 0);
		writeFullBytes(dh, byteIndex, dh.currentRecord, 0, myLogic.recordBytes);
	}

	protected final void writeFullBytes(DatabaseHandle dh, long byteIndex,
			byte[] recordArray, int off, int len) throws IOException {
		prepareWriteRange(dh, byteIndex, len);
		writeFullBytes(dh, recordArray, off, len);
	}

	public final void writeFullBytes(DatabaseHandle dh, byte[] byteArray,
			int off, int len) throws IOException {
		while (len > 0) {
			int written = writeBytes(dh, byteArray, off, len);
			if (written < 0)
				throw new EOFException();
			off += written;
			len -= written;
		}
	}

	public final void seek(DatabaseHandle dh, long recordIndex)
			throws IOException {
		assert dh.remainingBytes == 0;
		dh.location = myLogic.getByteIndex(recordIndex);
		dh.numBytes = -1;
		lowerSeek(dh, recordIndex);
	}

	public void lowerSeek(DatabaseHandle dh, long recordIndex)
			throws IOException {
	}

	public long readNextRecord(DatabaseHandle dh) throws IOException {
		readFullBytes(dh, dh.currentRecord, 0, myLogic.recordBytes);
		return myLogic.getRecord(dh.currentRecord, 0);
	}

	public void writeNextRecord(DatabaseHandle dh, long record)
			throws IOException {
		myLogic.fillBytes(record, dh.currentRecord, 0);
		writeFullBytes(dh, dh.currentRecord, 0, myLogic.recordBytes);
	}

	public void fill(DatabaseHandle dh, long record) throws IOException {
		seek(dh, firstRecordIndex);
		for (int i = 0; i < numRecords; i++) {
			writeNextRecord(dh, record);
		}
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

	public final int writeHeader(DataOutput out) throws IOException {
		out.writeLong(firstRecordIndex);
		out.writeLong(numRecords);
		return 16 + conf.store(out);
	}

	protected static class DatabaseArgs {
		final String dbClassString, uri;
		final Configuration conf;
		final long firstRecordIndex, numRecords;
		final boolean reading, writing;

		DatabaseArgs(String dbClassString, String uri, Configuration conf,
				long firstRecordIndex, long numRecords, boolean reading,
				boolean writing) {
			this.dbClassString = dbClassString;
			this.uri = uri;
			this.conf = conf;
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			this.reading = reading;
			this.writing = writing;
		}
	}

	public static Database openDatabase(String uri) throws IOException,
			ClassNotFoundException {
		return openDatabase(getArgs(uri));
	}

	public static DatabaseArgs getArgs(String uri) throws IOException,
			ClassNotFoundException {
		DataInputStream in = new DataInputStream(new FileInputStream(uri));
		long firstRecordIndex = in.readLong();
		long numRecords = in.readLong();
		Configuration conf = Configuration.load(in);
		in.close();
		return getArgs(uri, conf, firstRecordIndex, numRecords, true, false);
	}

	public static Database openDatabase(String uri, Configuration conf,
			boolean reading, boolean writing) throws IOException {
		return openDatabase(getArgs(uri, conf, reading, writing));
	}

	public static DatabaseArgs getArgs(String uri, Configuration conf,
			boolean reading, boolean writing) {
		Game<?> g = conf.getGame();
		return getArgs(uri, conf, 0, g.numHashes(), reading, writing);
	}

	public static DatabaseArgs getArgs(String uri, long firstRecordIndex,
			long numRecords, boolean reading, boolean writing)
			throws IOException, ClassNotFoundException {
		DataInputStream in = new DataInputStream(new FileInputStream(uri));
		Util.skipFully((DataInput) in, 16);
		Configuration conf = Configuration.load(in);
		in.close();
		return getArgs(uri, conf, firstRecordIndex, numRecords, reading,
				writing);
	}

	public static DatabaseArgs getArgs(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) {
		String dbClass = conf.getProperty("gamesman.database");
		if (writing) {
			String solveWrappers = conf.getProperty(
					"gamesman.database.writing.wrapper", "");
			if (!solveWrappers.isEmpty())
				dbClass = solveWrappers + ":" + dbClass;
		}
		return new DatabaseArgs(dbClass, uri, conf, firstRecordIndex,
				numRecords, reading, writing);
	}

	public static Database openDatabase(String dbClass, String uri,
			Configuration conf, long firstRecordIndex, long numRecords,
			boolean reading, boolean writing) throws IOException {
		return openDatabase(new DatabaseArgs(dbClass, uri, conf,
				firstRecordIndex, numRecords, reading, writing));
	}

	public static Database openDatabase(DatabaseArgs args) throws IOException {
		String[] classes = args.dbClassString.split(":");
		for (int i = 0; i < classes.length - 1; i++) {
			if (!classes[i].contains("."))
				classes[i] = "edu.berkeley.gamesman.database.wrapper."
						+ classes[i];
		}
		if (!classes[classes.length - 1].contains("."))
			classes[classes.length - 1] = "edu.berkeley.gamesman.database."
					+ classes[classes.length - 1];
		Database result;
		try {
			Class<? extends Database> underlying = Class.forName(
					classes[classes.length - 1]).asSubclass(Database.class);
			result = underlying.getConstructor(String.class,
					Configuration.class, Long.TYPE, Long.TYPE, Boolean.TYPE,
					Boolean.TYPE).newInstance(args.uri, args.conf,
					args.firstRecordIndex, args.numRecords, args.reading,
					args.writing);
			for (int i = classes.length - 2; i >= 0; i--) {
				Class<? extends DatabaseWrapper> next = Class.forName(
						classes[i]).asSubclass(DatabaseWrapper.class);
				result = next.getConstructor(Database.class,
						Configuration.class, Long.TYPE, Long.TYPE,
						Boolean.TYPE, Boolean.TYPE).newInstance(result,
						args.conf, args.firstRecordIndex, args.numRecords,
						args.reading, args.writing);
			}
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			else
				throw new Error(e.getCause());
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
		return result;
	}

	public final long requiredMemory(long numRecords) {
		return myLogic.getNumBytes(numRecords);
	}

	public static int skipHeader(DataInput in) throws IOException {
		Util.skipFully(in, 16);
		int confLength = in.readInt();
		Util.skipFully(in, confLength);
		return 20 + confLength;
	}

	/**
	 * @param numRecords
	 *            The number of records which need to be stored
	 * @return The number of bytes required to store numRecords records
	 */
	public long getNumBytes(long numRecords) {
		return myLogic.getNumBytes(numRecords);
	}

	/**
	 * @param numBytes
	 *            The number of bytes available
	 * @return The number of records which can be stored in numBytes bytes
	 */
	public long recordsForBytes(long numBytes) {
		return myLogic.getNumRecords(numBytes);
	}
}
