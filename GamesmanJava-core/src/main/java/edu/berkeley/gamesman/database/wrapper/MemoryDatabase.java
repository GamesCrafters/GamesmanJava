package edu.berkeley.gamesman.database.wrapper;

import java.io.EOFException;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.UnpreparedHandleException;
import edu.berkeley.gamesman.database.cache.RecordRangeCache;

public class MemoryDatabase extends DatabaseWrapper {
	private final RecordRangeCache recordCache;
	private final boolean writing;

	public MemoryDatabase(Database db, Configuration config, long firstRecord,
			long numRecords, boolean reading, boolean writing)
			throws IOException {
		super(db, config, firstRecord, numRecords, reading, writing);
		recordCache = new RecordRangeCache(myLogic);
		long numBytes = myLogic.getNumBytes(numRecords);
		if (numBytes > Integer.MAX_VALUE)
			throw new ArrayIndexOutOfBoundsException(
					"MemoryDatabase cannot hold more than 2 GB of records");
		recordCache.setRange(firstRecord, (int) numRecords);
		if (!writing) {
			DatabaseHandle dbHandle = db.getHandle(true);
			recordCache.readBytesFromDatabase(db, dbHandle,
					myLogic.getByteIndex(firstRecord), (int) numBytes);
		}
		this.writing = writing;
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		return recordCache.readBytes((int) location, array, off, len);
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		return recordCache.writeBytes((int) location, array, off, len);
	}

	@Override
	public long readNextRecord(DatabaseHandle dh) throws EOFException,
			UnpreparedHandleException {
		if (dh.numBytes < 0)
			throw new UnpreparedHandleException(dh);
		else if (dh.numBytes >= myLogic.recordBytes) {
			long record = readRecordFromByteIndex(dh, dh.location);
			incrementRecord(dh);
			return record;
		} else {
			throw new EOFException();
		}
	}

	@Override
	protected long readRecordFromByteIndex(DatabaseHandle dh, long byteIndex) {
		return recordCache.readRecordFromByteIndex(byteIndex);
	}

	@Override
	public void writeNextRecord(DatabaseHandle dh, long record)
			throws EOFException, UnpreparedHandleException {
		if (dh.numBytes < 0)
			throw new UnpreparedHandleException(dh);
		else if (dh.numBytes >= myLogic.recordBytes) {
			writeRecordFromByteIndex(dh, dh.location, record);
			incrementRecord(dh);
		} else {
			throw new EOFException();
		}
	}

	@Override
	protected void writeRecordFromByteIndex(DatabaseHandle dh, long byteIndex,
			long record) {
		recordCache.writeRecordFromByteIndex(byteIndex, record);
	}

	@Override
	public void close() throws IOException {
		if (writing) {
			DatabaseHandle dh = db.getHandle(false);
			recordCache.writeBytesToDatabase(db, dh,
					recordCache.getFirstByteIndex(), recordCache.getNumBytes());
			db.close();
		}
	}
}
