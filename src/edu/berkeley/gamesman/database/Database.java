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
import edu.berkeley.gamesman.database.util.DatabaseLogic;
import edu.berkeley.gamesman.database.wrapper.DatabaseWrapper;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Util;

/**
 * @author dnspies
 */
public abstract class Database implements Flushable, Closeable {

	public final Configuration conf;

	public final boolean reading;

	public final boolean writing;

	public final DatabaseLogic myLogic;

	public final long firstRecordIndex;

	public final long numRecords;

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

	public DatabaseHandle getHandle(boolean reading) {
		assert reading ? this.reading : this.writing;
		return new DatabaseHandle(myLogic.recordBytes, reading);
	}

	protected final long firstByteIndex() {
		return myLogic.getByteIndex(firstRecordIndex);
	}

	protected final long numBytes() {
		return myLogic.getNumBytes(numRecords);
	}

	public final void prepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
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

	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
	}

	public final void prepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
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

	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
	}

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

	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return readBytes(dh, dh.location, array, off, len);
	}

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

	public static Database openDatabase(String uri) throws IOException,
			ClassNotFoundException {
		DataInputStream in = new DataInputStream(new FileInputStream(uri));
		long firstRecordIndex = in.readLong();
		long numRecords = in.readLong();
		Configuration conf = Configuration.load(in);
		in.close();
		return openDatabase(uri, conf, firstRecordIndex, numRecords, true,
				false);
	}

	public static Database openDatabase(String uri, Configuration conf,
			boolean reading, boolean writing) throws IOException {
		Game<?> g = conf.getGame();
		return openDatabase(uri, conf, 0, g.numHashes(), reading, writing);
	}

	public static void openDatabase(String uri, long firstRecordIndex,
			long numRecords, boolean reading, boolean writing)
			throws IOException, ClassNotFoundException {
		DataInputStream in = new DataInputStream(new FileInputStream(uri));
		Util.skipFully((DataInput) in, 16);
		Configuration conf = Configuration.load(in);
		in.close();
		openDatabase(uri, conf, firstRecordIndex, numRecords, reading, writing);
	}

	public static Database openDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		String dbClass = conf.getProperty("gamesman.database");
		return openDatabase(dbClass, uri, conf, firstRecordIndex, numRecords,
				reading, writing);
	}

	public static Database openDatabase(String dbClassString, String uri,
			Configuration conf, long firstRecordIndex, long numRecords,
			boolean reading, boolean writing) throws IOException {
		String[] classes = dbClassString.split(":");
		for (int i = 0; i < classes.length; i++) {
			if (!classes[i].contains("."))
				classes[i] = "edu.berkeley.gamesman.database." + classes[i];
		}
		Database result;
		try {
			Class<? extends Database> underlying = Class.forName(
					classes[classes.length - 1]).asSubclass(Database.class);
			result = underlying.getConstructor(String.class,
					Configuration.class, Long.TYPE, Long.TYPE, Boolean.TYPE,
					Boolean.TYPE).newInstance(uri, conf, firstRecordIndex,
					numRecords, reading, writing);
			for (int i = classes.length - 2; i >= 0; i--) {
				Class<? extends DatabaseWrapper> next = Class.forName(
						classes[i]).asSubclass(DatabaseWrapper.class);
				result = next.getConstructor(Database.class,
						Configuration.class, Long.TYPE, Long.TYPE,
						Boolean.TYPE, Boolean.TYPE).newInstance(result, conf,
						firstRecordIndex, numRecords, reading, writing);
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

	public long getNumBytes(long numRecords) {
		return myLogic.getNumBytes(numRecords);
	}
}
